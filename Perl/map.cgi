#!/usr/bin/perl -T
# vim: tabstop=4 shiftwidth=4
# -----------------------------------------------------------------
# map.cgi -- The user interface for the UCSDMap.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# TODO:
#	- Create some kind of 'viewport' object to avoid passing around
# 	  these huge, indecipherable (and frequently-changing!) lists.
#
# $Id$
# -----------------------------------------------------------------

use strict;
use warnings;

# the first line is for packages installed on the server.
# the second is for all our own libraries
use lib qw(
	/home/jedimike/perl/lib/perl5/site_perl/5.8.3
	./lib
);

use CGI;
use FCGI;
use File::Temp ();
use HTML::Template;
use GD;
use POSIX qw(strftime);

# import lots of stuff from lots of different places...
use MapGlobals qw(TRUE FALSE INFINITY between asInt round getWords @SIZES @SCALES plog);
use LoadData qw(nameNormalize findLocation findKeyword isKeyword getKeyText);
use MapGraphics;
use ShortestPath;
use InterfaceLib qw(state listZoomLevels buildHelpText buildKeywordText
	buildLocationOptions buildLocationList pickZoom formatTime);


# we always need all the locations, so load them off disk
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);

# these may or may not be loaded later on
my($points);
my %dijkstra = ();

# -----------------------------------------------------------------
# Basic setup.
# -----------------------------------------------------------------

# make sure nobody gives us an absurd amount of input
$CGI::DISABLE_UPLOADS = 1;
$CGI::POST_MAX        = 1024; # 1k ought to be enough for anybody... ;)


# how far (in pixels) to pan when the user clicks "left", "right", "up", or
# "down" (but panning in two directions at once would move sqrt($pan+$pan),
# because I'm lazy)
my $pan = 100;

# start the FastCGI event loop
my $fcgi = FCGI::Request();
while( $fcgi->Accept() >= 0 ){

# -----------------------------------------------------------------
# Get input parameters.
# -----------------------------------------------------------------

my $q = new CGI($ENV{'QUERY_STRING'});

# source and destination location names
my $fromTxt = $q->param('from') || shift(@ARGV) || '';
my $toTxt   = $q->param('to')   || shift(@ARGV) || '';

# x and y display offsets, from the upper left
my $xoff   = asInt($q->param('xoff')   || 0);
my $yoff   = asInt($q->param('yoff')   || 0);
# the zoom level of the map; this is an index into the MapGlobals::SCALES
# array, which contains the actual scale multipliers
# this is undefined if no parameter was given (we go through hell later on to
# find a good default)
my $scale  = defined($q->param('scale')) ? asInt($q->param('scale')) : undef;
# reset the scale if we get a bogus value
$scale = 0 if( defined($scale) && !exists($SCALES[$scale]) );
# the size of the map display;  this is an index into MapGlobals::SIZES
my $size   = defined($q->param('size')) ? asInt($q->param('size')) : $MapGlobals::DEFAULT_SIZE;

# how fast do you walk?
# in minutes per mile.
my $mpm    = asInt($q->param('mpm'))  || $MapGlobals::DEFAULT_MPM;

# click offsets from the map
my $mapx = defined($q->param('map.x')) ? asInt($q->param('map.x')) : undef;
my $mapy = defined($q->param('map.y')) ? asInt($q->param('map.y')) : undef;

# click offsets from the thumbnail
my $thumbx = defined($q->param('thumb.x')) ? asInt($q->param('thumb.x')) : undef;
my $thumby = defined($q->param('thumb.y')) ? asInt($q->param('thumb.y')) : undef;
# one or the other (map or thumb click offsets) should be undef

# which template to use for output
my $template = getWords( $q->param('mode') || '' );
# we only care about the word characters in the template name (yeah, this is for taint mode)
$template = $MapGlobals::DEFAULT_TEMPLATE if( !exists($MapGlobals::TEMPLATES{$template}) );

# the base image
my $mapname = getWords( $q->param('mapname') || '' );
$mapname = $MapGlobals::DEFAULT_MAP if(!exists($MapGlobals::MAPS{$mapname}) );

# -----------------------------------------------------------------
# Do startup stuff: load data, convert some of the input data, initialize
# variables, etc.
# -----------------------------------------------------------------

# HTML-safe versions of the from and to text
my $fromTxtSafe = CGI::escapeHTML($fromTxt);
my $toTxtSafe = CGI::escapeHTML($toTxt);

# width and height of the viewing window (total image size is in MapGlobals)
my $width  = $SIZES[$size][0];
my $height = $SIZES[$size][1];

# kill old temporary files
MapGlobals::reaper($MapGlobals::DYNAMIC_IMG_DIR, $MapGlobals::DYNAMIC_MAX_AGE, $MapGlobals::DYNAMIC_IMG_SUFFIX);

my $ERROR = '';

# -----------------------------------------------------------------
# Attempt to use the user-provided values $fromTxt and $toTxt to find
# corresponding location IDs.
# -----------------------------------------------------------------

# keep track of whether source and destination locations were found
my ($src_found, $dst_found) = (TRUE, TRUE);
# keep track of whether we're doing a keyword search on either source or destination
my($src_keyword, $dst_keyword) = (FALSE, FALSE);
# any text associated with the operation of finding the source or destination
# (this is "did you mean...?" stuff)
my($src_help, $dst_help) = ('', '');

my ($from, $to) = (0, 0);
my @fromids = ();
my @toids = ();

# if we got no input, put up an informative message
if($fromTxt eq '' && $toTxt eq ''){
	($src_found, $dst_found) = (FALSE, FALSE);
}
# otherwise, attempt to look up both locations
else{

	##### this is for the destination location #####
	if( isKeyword($toTxt) ){
		# keyword matching
		if( !isKeyword($fromTxt) ){
			@toids = findKeyword(getKeyText($toTxt), $locations);
			$dst_keyword = TRUE;
		}
		else{
			# this is bad. we can't handle two keywords
			$ERROR .= "<p>Whoa there, mate! They can't <em>both</em> be keywords!</p>\n";
		}
		$dst_found = FALSE;
	}
	else{
		# regular matching
		@toids = findLocation($toTxt, $locations);
		# we found an exact match
		if(@toids == 1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$to = $toids[0]{'id'};
			$toTxt = $toids[0]{'text'};
			$toTxtSafe = CGI::escapeHTML($toTxt);
		}
		# we have multiple matches
		elsif(@toids > 1){
			$dst_found = FALSE;
		}
		# no matches
		else{
			# I've done all I can. He's dead, Jim.
			$ERROR .= "<p>Destination location &quot;$toTxtSafe&quot; not found.</p>\n" if($toTxt ne '');
			$dst_found = FALSE;
		}
	}

	##### this is for the source location #####
	if( isKeyword($fromTxt) ){
		# keyword matching
		if( !isKeyword($toTxt) ){
			@fromids = findKeyword(getKeyText($fromTxt), $locations);
			$src_keyword = TRUE;
		}
		# we don't print an error in an else{} here, because that
		# already happened above
		$src_found = FALSE;
	}
	else{
		@fromids = findLocation($fromTxt, $locations);
		# we found an exact match
		if(@fromids == 1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$from = $fromids[0]{'id'};
			$fromTxt = $fromids[0]{'text'};
			$fromTxtSafe = CGI::escapeHTML($fromTxt);
		}
		# we have multiple matches
		elsif(@fromids > 1){
			$src_found = FALSE;
		}
		# no matches
		else{
			$ERROR .= "<p>Start location &quot;$fromTxtSafe&quot; not found.</p>\n" if($fromTxt ne '');
			$src_found = FALSE;
		}
	}

}

# -----------------------------------------------------------------
# Now that we have source and destination IDs, build up some more variables
# based on that.
# -----------------------------------------------------------------

# build a list of printable location names
my $loc_opt = buildLocationList($locations);

# we actually run the shortest-path algorithm only if we found both
# the source and destination locations
my $havePath = ($src_found && $dst_found);

# URL-safe versions of the from and to text
my $fromTxtURL = CGI::escape($fromTxt);
my $toTxtURL = CGI::escape($toTxt);

# these store the _path_ IDs of the starting and ending point, which we
# need to actually find the shortest path
my $startID = 0;
my $endID = 0;

# if start and end locations are the same, don't draw
if( $from == $to ){
	$havePath = FALSE;
}
# otherwise, assign the start and end IDs to meaningful values
else{
	$startID = $locations->{'ByID'}{$from}{'PointID'};
	$endID = $locations->{'ByID'}{$to}{'PointID'};
}

# -----------------------------------------------------------------
# Figure out the shortest path between the two locations, if applicable. This
# is also where we handle the various cases such as when only one location is
# found but the other has multiple fuzzy possibilities, etc.
# -----------------------------------------------------------------

# do the shortest path stuff
my $dist = 0;
my $rect;
my $pathPoints;
if($havePath){
	
	# get the path between the two locations
	($dist, $rect, $pathPoints, $points) = LoadData::loadShortestPath(
		$locations->{'ByID'}{$from}, $locations->{'ByID'}{$to}, \%dijkstra, $points);
	
	# adjust the pixel distance to the unit we're using to display it (mi, ft, etc)
	if(defined($dist)){
		$dist /= $MapGlobals::PIXELS_PER_UNIT;
	}
	else{
		# if $dist is undefined, there's no path between the two
		# locations.
		$havePath = FALSE;
		$ERROR .= "These two locations are not connected. ";
	}

	# now pick a zoom level, if one hasn't already been specified
	if( !defined($scale) ){
		($scale, $xoff, $yoff) = pickZoom(
			$locations->{'ByID'}{$from}, $locations->{'ByID'}{$to},
			$xoff, $yoff, $width, $height, $rect, \@SCALES);
	}
}


# if we don't have a full path, check if we have a single location,
# and center on that
else{
	# error if we got no matches
	if(@fromids == 0){
		if($src_keyword){
			$ERROR .= "<p><b>&quot;" . getKeyText($fromTxtSafe) . "&quot; is not a valid keyword.</b></p>\n";
		}
	}
	# if we got multiple matches, build help text to disambiguate
	elsif(@fromids > 1){
		# if we found a good 'from' location, run shortest path stuff
		# so we can display distances
		if($dst_found){
			$points	= LoadData::loadPoints($MapGlobals::POINT_FILE) unless defined($points);
			# get the result of Dijkstra's somehow
			if( !defined( $dijkstra{$endID})
					&& !defined($dijkstra{$endID} = LoadData::readDijkstraCache($endID)) ){
				$dijkstra{$endID} = ShortestPath::find($endID, $points);
				LoadData::writeDijkstraCache($dijkstra{$endID}, $endID);
			}
		}
		if($src_keyword){
			$src_help = "<p><b>Closest matches for $fromTxtSafe...</b></p>"
				. buildKeywordText(undef, $toTxtURL, $mpm, $template, $locations, $points, $dijkstra{$endID}, \@fromids);
		}
		else{
			$src_help = "<p><b>Start location &quot;$fromTxtSafe&quot; not found.</b></p>"
				. buildHelpText(undef, $toTxtURL, $mpm, $template, $locations, $points, $dijkstra{$endID}, \@fromids);
		}
	}

	if(@toids == 0){
		if($dst_keyword){
			$ERROR .= "<p><b>&quot;" . getKeyText($toTxtSafe) . "&quot; is not a valid keyword.</b></p>\n";
		}
	}
	elsif(@toids > 1){
		# if we found a good 'to' location, run shortest path stuff
		# so we can display distances
		if($src_found){
			$points	= LoadData::loadPoints($MapGlobals::POINT_FILE) unless defined($points);
			# get the result of Dijkstra's somehow
			if( !defined( $dijkstra{$startID})
					&& !defined($dijkstra{$startID} = LoadData::readDijkstraCache($startID)) ){
				$dijkstra{$startID} = ShortestPath::find($startID, $points);
				LoadData::writeDijkstraCache($dijkstra{$startID}, $startID);
			}
		}
		if($dst_keyword){
			$dst_help = "<p><b>Closest matches for $toTxtSafe...</b></p>"
				. buildKeywordText($fromTxtURL, undef, $mpm, $template, $locations, $points, $dijkstra{$startID}, \@toids);
		}
		else{
			$dst_help = "<p><b>Destination location &quot;$toTxtSafe&quot; not found.</b></p>"
				. buildHelpText($fromTxtURL, undef, $mpm, $template, $locations, $points, $dijkstra{$startID}, \@toids);
		}
	}

	if(!$xoff && !$yoff){
		if($src_found){
			$xoff = $locations->{'ByID'}{$from}{'x'};
			$yoff = $locations->{'ByID'}{$from}{'y'};
		}
		elsif($dst_found){
			$xoff = $locations->{'ByID'}{$to}{'x'};
			$yoff = $locations->{'ByID'}{$to}{'y'};
		}
	}

	# use the default scale
	if(!defined($scale)){
		# we use different levels of zoom depending on whether we're
		# not focused on _anything_, or if the user selected a single location
		if($src_found || $dst_found){
			$scale = $MapGlobals::SINGLE_LOC_SCALE;
		}
		else{
			$scale = $MapGlobals::DEFAULT_SCALE;
		}
	}
}

# if we still don't have offsets, use the default ones
if(!$xoff && !$yoff){
	$xoff = $MapGlobals::DEFAULT_XOFF;
	$yoff = $MapGlobals::DEFAULT_YOFF;
}

# these coordinates are the "visible" x/y offsets: that is, the center of what
# you actually see on the map (which can't be _too_ close to the edges of the
# map). e.g., even if the "real" center is at (0,0), these will be at (width/2,
# height/2). these are used for the panning buttons.
# the rule of thumb is: anything that MOVES the view should use the norm_*
# offsets, anything that PRESERVES it should use the regular ones.
my $norm_xoff = between(($width/$SCALES[$scale])/2, $MapGlobals::IMAGE_X - ($width/$SCALES[$scale])/2, $xoff);
my $norm_yoff = between(($height/$SCALES[$scale])/2, $MapGlobals::IMAGE_Y - ($height/$SCALES[$scale])/2, $yoff);

# integrate the image click offsets (if the image was clicked to recenter)
if( defined($mapx) && defined($mapy) ){
	# adjust the map offsets so they work from the center of the screen
	$mapx -= $width/2;
	$mapy -= $height/2;

	# now adjust our absolute x/y offsets to take into account the last click location
	$xoff = $norm_xoff + $mapx/$SCALES[$scale];
	$yoff = $norm_yoff + $mapy/$SCALES[$scale];
}
# if we got a thumbnail click, it's a little easier
elsif( defined($thumbx) && defined($thumbx) ){
	$xoff = int($thumbx/$MapGlobals::RATIO_X);
	$yoff = int($thumby/$MapGlobals::RATIO_Y);
}

# and now that we _changed_ them, we've got to make sure the offsets are
# in-bounds again: $xoff and $yoff just have to be somewhere inside the map;
# $norm_xoff and $norm_yoff must be valid locations for the center of the
# screen (just like before)
$xoff = between(0, $MapGlobals::IMAGE_X, $xoff);
$yoff = between(0, $MapGlobals::IMAGE_Y, $yoff);
$norm_xoff = between(($width/$SCALES[$scale])/2, $MapGlobals::IMAGE_X - ($width/$SCALES[$scale])/2, $xoff);
$norm_yoff = between(($height/$SCALES[$scale])/2, $MapGlobals::IMAGE_Y - ($height/$SCALES[$scale])/2, $yoff);

# these are coordinates of the upper-left-hand corner, used by low-level
# drawing functions.
my $rawxoff = int($norm_xoff*$SCALES[$scale] - $width/2);
my $rawyoff = int($norm_yoff*$SCALES[$scale] - $height/2);

# make sure the offsets don't go out of range
$rawxoff = between(0, $MapGlobals::IMAGE_X - $width, $rawxoff);
$rawyoff = between(0, $MapGlobals::IMAGE_Y - $height, $rawyoff);

# -----------------------------------------------------------------
# Open the map image and draw to it. Note that the draw statements are ordered
# so that the various elements on the map will be layered correctly -- they are
# NOT necesarily in logical order.
# -----------------------------------------------------------------

# if we're using the plain template, write output images
my($tmpfile, $tmpthumb);
if($template eq 'plain' || $template eq 'print'){
	$tmpfile = MapGraphics::makeMapImage($locations,
		$havePath ? $pathPoints : undef, $mapname,
		$src_found ? $from : undef,
		$dst_found ? $to : undef,
		$width, $height, $rawxoff, $rawyoff, $scale);

}

# -----------------------------------------------------------------
# If we're in print view, draw the two close-up views.
# -----------------------------------------------------------------
my($little_window_src, $little_window_dst);
if($template eq 'print'){
	if($src_found){
		$little_window_src = MapGraphics::makeZoomImage($locations, $havePath ? $pathPoints : undef,
			$mapname, $from, \@MapGlobals::SRC_COLOR);
	}
	if($dst_found){
		$little_window_dst = MapGraphics::makeZoomImage($locations, $havePath ? $pathPoints : undef,
			$mapname, $to, \@MapGlobals::DST_COLOR);
	}
}

# -----------------------------------------------------------------
# Create the thumbnail (if we're in the plain view).
# -----------------------------------------------------------------
if($template eq 'plain'){

	# yes, I'm using ternary operators in the middle of a function call. hah.
	$tmpthumb = MapGraphics::makeThumbnail($src_found ? $from : undef, $dst_found ? $to : undef,
		$scale, $width, $height, $norm_xoff, $norm_yoff, $locations);

}
# -----------------------------------------------------------------
# Create the path images (if we're in javascript view).
# -----------------------------------------------------------------
my($pathImgRect);
# if we're using the javascript template, write scaled path images
if ($template eq 'js'){
	if($havePath){
		# create the path images for this path at all scales
		$pathImgRect = MapGraphics::makePathImages($from, $to, $rect, $pathPoints);
	}
}

# -----------------------------------------------------------------
# Feed the values into the template.
# -----------------------------------------------------------------

# now we slam everything into a template and print it out
my $tmpl = HTML::Template->new(
	filename => $MapGlobals::TEMPLATES{$template},
	die_on_bad_params => 0,
	global_vars => 1
);


# basic info: who we are, where to find images, etc
$tmpl->param( SELF => $MapGlobals::SELF ); # whoooooooooo are you?

# paths
$tmpl->param( HTML_DIR => $MapGlobals::HTML_BASE );
$tmpl->param( IMG_DIR => $MapGlobals::STATIC_IMG_DIR );
$tmpl->param( CSS_DIR => $MapGlobals::CSS_DIR );

# info about current state
$tmpl->param( SCALE => $scale );
$tmpl->param( SIZE => $size );
$tmpl->param( MPM => $mpm );
$tmpl->param( MODE => $template );
$tmpl->param( XOFF => $xoff );
$tmpl->param( YOFF => $yoff );
$tmpl->param( WIDTH => $width );
$tmpl->param( HEIGHT => $height );
$tmpl->param( MAP_NAME => $mapname );

# user's input text
$tmpl->param( TXT_SRC => $fromTxtSafe );
$tmpl->param( TXT_DST => $toTxtSafe );

# official name of the from/to locations, OR the input text (same as TXT_SRC
# and TXT_DST) if there are no from/to locations
$tmpl->param( TXT_SRC_OFFICIAL => $from
	? CGI::escapeHTML($locations->{'ByID'}{$from}{'Name'})
	: $fromTxtSafe );
$tmpl->param( TXT_DST_OFFICIAL => $to
	? CGI::escapeHTML($locations->{'ByID'}{$to}{'Name'})
	: $toTxtSafe );

# this is the big list of locations (<option> tags)
$tmpl->param( LOCATION_OPT =>  $loc_opt);

# helper and error text
$tmpl->param( TXT_ERROR => $ERROR );
$tmpl->param( SRC_HELP => $src_help );
$tmpl->param( DST_HELP => $dst_help );

# boolean values, for whatever strange logic we may need inside the template
$tmpl->param( SRC_FOUND => $src_found );
$tmpl->param( DST_FOUND => $dst_found );
$tmpl->param( PATH_FOUND => $havePath );

# this is tells whether we're actually displaying a path between two separate locations
$tmpl->param( GOT_PATH => $havePath );
$tmpl->param( DISTANCE => sprintf("%.02f", $dist || 0) );
$tmpl->param( TIME => formatTime(($dist||0)*$mpm) );

# link to the print view
$tmpl->param( PRINT_URL => 
	"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $norm_xoff, $norm_yoff, $scale, $size, $mpm, 'print'));

# the simple interface
if($template eq 'plain'){
	# filenames for the temporary thumb and map files
	$tmpl->param( IMG_VIEW => $tmpfile );
	$tmpl->param( IMG_THUMB => $tmpthumb );

	$tmpl->param( ZOOM_WIDGET =>
		listZoomLevels($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, $size, $mpm, $template));

	# URLs for various operations
	$tmpl->param( UP_URL => 
		"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $norm_xoff, $norm_yoff - $pan/$SCALES[$scale], $scale, $size, $mpm, $template));
	$tmpl->param( DOWN_URL => 
		"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $norm_xoff, $norm_yoff + $pan/$SCALES[$scale], $scale, $size, $mpm, $template));
	$tmpl->param( LEFT_URL => 
		"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $norm_xoff - $pan/$SCALES[$scale], $norm_yoff, $scale, $size, $mpm, $template));
	$tmpl->param( RIGHT_URL => 
		"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $norm_xoff + $pan/$SCALES[$scale], $norm_yoff, $scale, $size, $mpm, $template));
	#$tmpl->param( SMALLER_URL => 
	#	"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $norm_xoff, $norm_yoff, $scale, ($size > 0) ? $size-1 : $size, $mpm, $template));
	#$tmpl->param( BIGGER_URL => 
	#	"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $norm_xoff, $norm_yoff, $scale, ($size < $#SIZES) ? $size+1 : $size, $mpm, $template));

	$tmpl->param( ZOOM_OUT_URL => "$MapGlobals::SELF?" . state($fromTxtSafe, $toTxtSafe, $xoff, $yoff,
		($scale < $#MapGlobals::SCALES) ? $scale + 1 : $#MapGlobals::SCALES, $size, $mpm, $template));
	$tmpl->param( ZOOM_IN_URL => "$MapGlobals::SELF?" . state($fromTxtSafe, $toTxtSafe, $xoff, $yoff,
		($scale > 0) ? $scale - 1 : 0, $size, $mpm, $template));

	# zooming to the start or end locations
	if($src_found){
		$tmpl->param( GOTO_SRC_URL => "$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $locations->{'ByID'}{$from}{'x'}, $locations->{'ByID'}{$from}{'y'}, $MapGlobals::SINGLE_LOC_SCALE, $size, $mpm, $template));
	}
	if($dst_found){
		$tmpl->param( GOTO_DST_URL => "$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, $locations->{'ByID'}{$to}{'x'}, $locations->{'ByID'}{$to}{'y'}, $MapGlobals::SINGLE_LOC_SCALE, $size, $mpm, $template));
	}

	$tmpl->param( RECENTER_URL => 
		"$MapGlobals::SELF?" . state($fromTxtURL, $toTxtURL, undef, undef, undef, $size, $mpm, $template));
}
# the javascript draggy view
elsif($template eq 'js'){
	# more directories to store our stuff
	$tmpl->param( GRID_DIR => $MapGlobals::GRID_IMG_DIR );
	$tmpl->param( PATHS_DIR => $MapGlobals::PATH_IMG_DIR );

	# we need info about each location if it's found
	if($src_found){
		$tmpl->param( SRC_NAME => $locations->{'ByID'}{$from}{'Name'} );
		$tmpl->param( SRC_X => $locations->{'ByID'}{$from}{'x'} );
		$tmpl->param( SRC_Y => $locations->{'ByID'}{$from}{'y'} );
	}
	if($dst_found){
		$tmpl->param( DST_NAME => $locations->{'ByID'}{$to}{'Name'} );
		$tmpl->param( DST_X => $locations->{'ByID'}{$to}{'x'} );
		$tmpl->param( DST_Y => $locations->{'ByID'}{$to}{'y'} );
	}

	# info about the path
	if($havePath){
		$tmpl->param( PATH_X => $pathImgRect->{'xmin'} );
		$tmpl->param( PATH_Y => $pathImgRect->{'ymin'} );
		$tmpl->param( PATH_W => $pathImgRect->{'xmax'} - $pathImgRect->{'xmin'} );
		$tmpl->param( PATH_H => $pathImgRect->{'ymax'} - $pathImgRect->{'ymin'} );
		$tmpl->param( PATH_DIST => $dist );
		$tmpl->param( PATH_SRC => $from );
		$tmpl->param( PATH_DST => $to );
	}
}
# the print screen
elsif($template eq 'print'){
	# close-up views of each location, if applicable
	if($src_found){
		$tmpl->param( IMG_SRC => $little_window_src );
	}
	if($dst_found){
		$tmpl->param( IMG_DST => $little_window_dst );
	}

	# what's the time?
	$tmpl->param( TXT_NOW => strftime("%r %A, %B %d, %Y", localtime()) );

	# main view, just like the plain template
	$tmpl->param( IMG_VIEW => $tmpfile );
}


# -----------------------------------------------------------------
# Everything is finally sent to the browser
# -----------------------------------------------------------------

print "Content-type: text/html\n\n" . $tmpl->output();

} # end FastCGI event loop

# that's all, folks!
