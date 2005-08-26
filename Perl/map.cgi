#!/usr/bin/perl -T
# -----------------------------------------------------------------
# map.cgi -- The user interface for the UCSDMap.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# TODO:
#	- Create some kind of 'viewport' object to avoid passing around
# 	  these huge, indecipherable (and frequently-changing!) lists.
#	- move as many subroutines as possible out of this file
#	- check loading and searching of $points: is it being done more often
#	  than necessary on double fuzzy matches?
#
# Wed Aug 24 20:45:24 PDT 2005
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
use File::Temp ();
use HTML::Template;
use GD;

use MapGlobals qw(TRUE FALSE INFINITY between asInt round @SIZES @SCALES);
use LoadData qw(nameNormalize findLocation findKeyword isKeyword getKeyText);
use MapGraphics;
use ShortestPath;
use InterfaceLib qw(state listZoomLevels buildHelpText buildKeywordText
	buildLocationOptions buildLocationList pickZoom formatTime);

# -----------------------------------------------------------------
# Basic setup.
# -----------------------------------------------------------------

# make sure nobody gives us an absurd amount of input
$CGI::DISABLE_UPLOADS = 1;
$CGI::POST_MAX        = 1024; # 1k ought to be enough for anybody... ;)

my $q = new CGI();

# how far (in pixels) to pan when the user clicks "left", "right", "up", or
# "down"
my $pan = 100;

# -----------------------------------------------------------------
# Get input parameters.
# -----------------------------------------------------------------
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
# the size of the map display;  this is an index into MapGlobals::SIZES
my $size   = asInt(defined($q->param('size')) ? $q->param('size') : 1);

# how fast do you walk?
# in minutes per mile.
my $mpm    = asInt($q->param('mpm'))  || 15;

# click offsets from the map
my $mapx = defined($q->param('map.x')) ? asInt($q->param('map.x')) : undef;
my $mapy = defined($q->param('map.y')) ? asInt($q->param('map.y')) : undef;

# click offsets from the thumbnail
my $thumbx = defined($q->param('thumb.x')) ? asInt($q->param('thumb.x')) : undef;
my $thumby = defined($q->param('thumb.y')) ? asInt($q->param('thumb.y')) : undef;
# one or the other (map or thumb click offsets) should be undef

# which template to use for output
my $template = $q->param('mode') || '';
# we only care about the word characters in the template name (this is for taint mode)
$template =~ /^(\w+)/;
$template = $1;
$template = 'js' if(!length($template || '') || !exists($MapGlobals::TEMPLATES{$template}));

# -----------------------------------------------------------------
# Do startup stuff: load data, convert some of the input data, initialize
# variables, etc.
# -----------------------------------------------------------------

# we always need all the locations, so load them off disk
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);

# these may or may not be loaded later on
my($points, $edgeFH, $edgeSize);

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
			$toTxt = $locations->{'ByID'}{$to}{'Name'};
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
			$fromTxt = $locations->{'ByID'}{$from}{'Name'};
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

	# first, check if there's a cache file for the path we want
	my $cachefile = MapGlobals::getCacheName($from, $to);

	if( -e $cachefile ){
		# yay! we have a cache file. life is good. :)
		($dist, $rect, $pathPoints) = LoadData::loadCache($cachefile);
	}
	else{
		# nothing cached, load everything from disk :(
		$points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
		($edgeFH, $edgeSize) = LoadData::initEdgeFile($MapGlobals::EDGE_FILE);

		ShortestPath::find($startID, $points);

		($dist, $rect, $pathPoints) = ShortestPath::pathPoints($points, $edgeFH, $edgeSize,
			$points->{$startID}, $points->{$endID});
		LoadData::writeCache($cachefile, $dist, $rect, $pathPoints);

		# if we created a cache file, we're responsible for clearing
		# out any old ones too
		MapGlobals::reaper($MapGlobals::CACHE_DIR, $MapGlobals::CACHE_EXPIRY, '.cache');
	}
	
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
			$points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
			ShortestPath::find($endID, $points);
		}
		if($src_keyword){
			$src_help = "<p><b>Closest matches for $fromTxtSafe...</b></p>"
				. buildKeywordText(undef, $toTxtURL, $mpm, $template, $locations, $points, \@fromids);
		}
		else{
			$src_help = "<p><b>Start location &quot;$fromTxtSafe&quot; not found.</b></p>"
				. buildHelpText(undef, $toTxtURL, $mpm, $template, $locations, $points, \@fromids);
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
			$points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
			ShortestPath::find($startID, $points);
		}
		if($dst_keyword){
			$dst_help = "<p><b>Closest matches for $toTxtSafe...</b></p>"
				. buildKeywordText($fromTxtURL, undef, $mpm, $template, $locations, $points, \@toids);
		}
		else{
			$dst_help = "<p><b>Destination location &quot;$toTxtSafe&quot; not found.</b></p>"
				. buildHelpText($fromTxtURL, undef, $mpm, $template, $locations, $points, \@toids);
		}
	}

	if($src_found){
		$xoff = $locations->{'ByID'}{$from}{'x'};
		$yoff = $locations->{'ByID'}{$from}{'y'};
	}
	elsif($dst_found){
		$xoff = $locations->{'ByID'}{$to}{'x'};
		$yoff = $locations->{'ByID'}{$to}{'y'};
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

# integrate the image click offsets (if the image was clicked to recenter)
if( defined($mapx) && defined($mapy) ){
	# adjust the map offsets so they work from the center of the screen
	$mapx -= $width/2;
	$mapy -= $height/2;

	# now adjust our absolute x/y offsets to take into account the last click location
	$xoff += $mapx/$SCALES[$scale];
	$yoff += $mapy/$SCALES[$scale];
}
# if we got a thumbnail click, it's a little easier
elsif( defined($thumbx) && defined($thumbx) ){
	$xoff = int($thumbx/$MapGlobals::RATIO_X);
	$yoff = int($thumby/$MapGlobals::RATIO_Y);
}

# make sure the x/y offsets are in range, and correct them if they aren't
# (but only AFTER we remember them for the links dependent on the current, unadjusted state)
$xoff = between(0, $MapGlobals::IMAGE_X, $xoff);
$yoff = between(0, $MapGlobals::IMAGE_Y, $yoff);

# these coordinates are the "visible" x/y offsets: that is, the center of what
# you actually see on the map (which can't be _too_ close to the edges of the
# map). e.g., even if the "real" center is at (0,0), these will be at (width/2,
# height/2). these are used for the panning buttons.
# the rule of thumb is: anything that MOVES the view should use the norm_*
# offsets, anything that PRESERVES it should use the regular ones.
my $norm_xoff = between(($width/$SCALES[$scale])/2, $MapGlobals::IMAGE_X - ($width/$SCALES[$scale])/2, $xoff);
my $norm_yoff = between(($height/$SCALES[$scale])/2, $MapGlobals::IMAGE_Y - ($height/$SCALES[$scale])/2, $yoff);

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
# NOT necesarily in a logical order.
# -----------------------------------------------------------------

# if we're using the plain template, write output images
my($tmpfile, $tmpthumb);
my($pathImgRect);
if($template eq 'plain'){

	# get the image of the appropriate scale from disk, and grab only
	# what we need by size and offset
	my $im = GD::Image->newFromGd2Part(MapGlobals::getGd2Filename($SCALES[$scale]),
		$rawxoff, $rawyoff, $width, $height)
		|| die "Could not load base image " . MapGlobals::getGd2Filename($SCALES[$scale]) . "\n";

	my $src_color = $im->colorAllocate(@MapGlobals::SRC_COLOR);
	my $dst_color = $im->colorAllocate(@MapGlobals::DST_COLOR);
	my $path_color = $im->colorAllocate(@MapGlobals::PATH_COLOR);
	my $bg_color = $im->colorAllocate(@MapGlobals::LOC_BG_COLOR);

	# uncomment to draw ALL locations
	#MapGraphics::drawAllLocations($locations, $im, $src_color, $src_color, $bg_color, $rawxoff, $rawyoff,
	#				$width, $height, $SCALES[$scale]);

	# if we had a path to draw, now's the time to do it
	if($havePath){
		foreach my $line (@$pathPoints){
			MapGraphics::drawLines($line, $im, $MapGlobals::PATH_THICKNESS, $path_color, $rawxoff, $rawyoff,
				$width, $height, $SCALES[$scale]);
		}
	}

	# draw the source and destination locations, if they've been found
	if($src_found){
		MapGraphics::drawLocation($locations->{'ByID'}{$from}, $im, $src_color, $src_color, $bg_color,
			$rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);
	}
	if($dst_found){
		MapGraphics::drawLocation($locations->{'ByID'}{$to}, $im, $dst_color, $dst_color, $bg_color,
			$rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);
	}


	# generate a temporary file on disk to store the map image
	$tmpfile = new File::Temp(
			TEMPLATE => 'ucsdmap-XXXXXX',
			DIR => $MapGlobals::DYNAMIC_IMG_DIR,
			SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
			UNLINK => 0,
		);
	chmod(0644, $tmpfile->filename);

	# print the data out to a temporary file
	binmode($tmpfile);
	print $tmpfile $im->png();
	close($tmpfile);


	# -----------------------------------------------------------------
	# Draw to the thumbnail.
	# -----------------------------------------------------------------

	# grab the thumbnail from disk
	my $thumb = GD::Image->newFromGd2($MapGlobals::THUMB_FILE);

	# store the ratio between the thumbnail and the main base image
	# (these two REALLY should be the same...)
	my $ratio_x = $MapGlobals::THUMB_X / $MapGlobals::IMAGE_X;
	my $ratio_y = $MapGlobals::THUMB_Y / $MapGlobals::IMAGE_Y;

	# this is the color in which we draw the edge-of-view lines
	my $thumb_src_color = $thumb->colorAllocate(@MapGlobals::SRC_COLOR);
	my $thumb_dst_color = $thumb->colorAllocate(@MapGlobals::DST_COLOR);
	my $thumb_rect_color = $thumb->colorAllocate(@MapGlobals::RECT_COLOR);

	# the outline of the current view
	$thumb->rectangle(
		($norm_xoff - ($width/$SCALES[$scale])/2)*$MapGlobals::RATIO_X,
		($norm_yoff - ($height/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y,
		($norm_xoff + ($width/$SCALES[$scale])/2)*$MapGlobals::RATIO_X - 1,
		($norm_yoff + ($height/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y - 1,
		$thumb_rect_color
	);

	# dots for the start and end locations
	if($src_found){
		$thumb->filledRectangle(
			$locations->{'ByID'}{$from}{'x'}*$MapGlobals::RATIO_X - 1,
			$locations->{'ByID'}{$from}{'y'}*$MapGlobals::RATIO_Y - 1,
			$locations->{'ByID'}{$from}{'x'}*$MapGlobals::RATIO_X + 1,
			$locations->{'ByID'}{$from}{'y'}*$MapGlobals::RATIO_Y + 1,
			$thumb_src_color,
		);
	}

	if($dst_found){
		$thumb->filledRectangle(
			$locations->{'ByID'}{$to}{'x'}*$MapGlobals::RATIO_X - 1,
			$locations->{'ByID'}{$to}{'y'}*$MapGlobals::RATIO_Y - 1,
			$locations->{'ByID'}{$to}{'x'}*$MapGlobals::RATIO_X + 1,
			$locations->{'ByID'}{$to}{'y'}*$MapGlobals::RATIO_Y + 1,
			$thumb_dst_color,
		);
	}

	# now make a temporary file to put this image in
	# XXX: eventually just make this a separate CGI script?
	# I don't know -- which has higher cost: creating/deleting a file, or starting
	# another perl process?
	$tmpthumb = new File::Temp(
			TEMPLATE => 'ucsdmap-XXXXXX',
			DIR => $MapGlobals::DYNAMIC_IMG_DIR,
			SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
			UNLINK => 0,
		);
	chmod(0644, $tmpthumb->filename);

	# write out the finished thumbnail to the file
	binmode($tmpthumb);
	print $tmpthumb $thumb->png();
	close($tmpthumb);

	# while we're at it, close the edge file that was opened with initEdgeFile
	if( defined($edgeFH) ){
		close($edgeFH);
	}
}
# if we're using the javascript template, write scaled path images
elsif ($template eq 'js'){

	if($havePath){

		# a little padding 
		my $padding = 8;
		# if we have a path between two locations, write the path images
		my $curScale;
		
		$pathImgRect->{'xmin'} = $rect->{'xmin'} - $padding;
		$pathImgRect->{'ymin'} = $rect->{'ymin'} - $padding;
		$pathImgRect->{'xmax'} = $rect->{'xmax'} + $padding;
		$pathImgRect->{'ymax'} = $rect->{'ymax'} + $padding;
		my $pathWidth = $pathImgRect->{'xmax'} - $pathImgRect->{'xmin'};
		my $pathHeight = $pathImgRect->{'ymax'} - $pathImgRect->{'ymin'};

		for my $i (0..$#SCALES){
			$curScale = $SCALES[$i];

			if(! -e MapGlobals::getPathFilename($from, $to, $i) ){
				# since we're creating new ones, delete old path files
				MapGlobals::reaper($MapGlobals::PATH_IMG_DIR, $MapGlobals::PATH_MAX_AGE, $MapGlobals::DYNAMIC_IMG_SUFFIX);

				#print "generating scale $i: $curScale\n";
				
				my $im = GD::Image->new($pathWidth*$curScale, $pathHeight*$curScale);
				my $bg_color = $im->colorAllocate(0, 0, 0);
				$im->transparent($bg_color);
				my $path_color = $im->colorAllocate(@MapGlobals::PATH_COLOR);

				foreach my $line (@$pathPoints){
					MapGraphics::drawLinesRaw($line, $im,
						$MapGlobals::PATH_THICKNESS, $path_color,
						$rect->{'xmin'} - $padding,
						$rect->{'ymin'} - $padding,
						$pathWidth, $pathHeight, $curScale);
				}

				open(OUTFILE, '>', MapGlobals::getPathFilename($from, $to, $i)) or die "cannot open output file: $!\n";
				binmode(OUTFILE);
				print OUTFILE $im->png();
				close(OUTFILE);
			}
		}
	}

}

# -----------------------------------------------------------------
# Feed the values into the template.
# (A LOT of these are commented-out because we don't use them right now.)
# -----------------------------------------------------------------

# now we slam everything into a template and print it out
my $tmpl = HTML::Template->new(
	filename => $MapGlobals::TEMPLATES{$template},
	die_on_bad_params => 0,
	global_vars => 1
);

# this is stuff for the plain view
# XXX: merge the code for these two views.
if($template eq 'plain'){

	# basic info: who we are, where to find images, etc
	$tmpl->param( SELF => $MapGlobals::SELF ); # whoooooooooo are you?

	# CVS tags
	#$tmpl->param( CVS_ID => '$Id$');
	$tmpl->param( CVS_REVISION => '$Revision$');
	#$tmpl->param( CVS_DATE => '$Date$');
	#$tmpl->param( CVS_AUTHOR => '$Author$');

	# filenames for the temporary thumb and map files
	$tmpl->param( IMG_VIEW => $tmpfile->filename );
	$tmpl->param( IMG_THUMB => $tmpthumb->filename );

	# static content directories
	$tmpl->param( HTML_DIR => $MapGlobals::HTML_BASE );
	$tmpl->param( IMG_DIR => $MapGlobals::STATIC_IMG_DIR );
	$tmpl->param( CSS_DIR => $MapGlobals::CSS_DIR );

	# add info about current state
	$tmpl->param( SCALE => $scale );
	$tmpl->param( SIZE => $size );
	$tmpl->param( MPM => $mpm );
	$tmpl->param( MODE => $template );
	$tmpl->param( XOFF => $xoff );
	$tmpl->param( YOFF => $yoff );
	$tmpl->param( WIDTH => $width );
	$tmpl->param( HEIGHT => $height );
	#$tmpl->param( THUMB_WIDTH => $MapGlobals::THUMB_X );
	#$tmpl->param( THUMB_HEIGHT => $MapGlobals::THUMB_Y );

	$tmpl->param( ZOOM_WIDGET =>
		listZoomLevels($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, $size, $mpm, $template));

	$tmpl->param( LOCATION_OPT =>  $loc_opt);

	# the strings representing the state of various buttons
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

	# text
	$tmpl->param( TXT_SRC => $fromTxtSafe );
	$tmpl->param( TXT_DST => $toTxtSafe );
	$tmpl->param( TXT_ERROR => $ERROR );

	# this is tells whether we're actually displaying a path between two separate locations
	$tmpl->param( GOT_PATH => $havePath );
	$tmpl->param( DISTANCE => sprintf("%.02f", $dist || 0) );
	$tmpl->param( TIME => formatTime(($dist||0)*$mpm) );

	# a bunch of boolean values, for whatever strange logic we may need inside the template
	$tmpl->param( SRC_FOUND => $src_found );
	$tmpl->param( DST_FOUND => $dst_found );

	# helper text for searching
	$tmpl->param( SRC_HELP => $src_help );
	$tmpl->param( DST_HELP => $dst_help );
	#$tmpl->param( SRC_OR_DST_FOUND => ($src_found || $dst_found) );
	#$tmpl->param( SRC_AND_DST_FOUND => ($src_found && $dst_found) );
	#$tmpl->param( SRC_AND_DST_BLANK => ($fromTxt eq '' && $toTxt eq '') );

	# hex triplets representing the colors for the source and destination locations
	#$tmpl->param( SRC_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::SRC_COLOR));
	#$tmpl->param( DST_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::DST_COLOR));
	#$tmpl->param( PATH_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::PATH_COLOR));
	#$tmpl->param( BG_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::LOC_BG_COLOR));
	$tmpl->param( CSS_FILE => $MapGlobals::CSS_FILE );

}
# output variables for the javascript template
elsif ($template eq 'js'){

	# basic info: who we are, where to find images, etc
	$tmpl->param( SELF => $MapGlobals::SELF ); # whoooooooooo are you?

	# CVS tags
	#$tmpl->param( CVS_ID => '$Id$');
	$tmpl->param( CVS_REVISION => '$Revision$');
	#$tmpl->param( CVS_DATE => '$Date$');
	#$tmpl->param( CVS_AUTHOR => '$Author$');

	$tmpl->param( HTML_DIR => $MapGlobals::HTML_BASE );
	$tmpl->param( IMG_DIR => $MapGlobals::STATIC_IMG_DIR );
	$tmpl->param( PATHS_DIR => $MapGlobals::PATH_IMG_DIR );
	$tmpl->param( GRID_DIR => $MapGlobals::GRID_IMG_DIR );
	$tmpl->param( CSS_DIR => $MapGlobals::CSS_DIR );

	$tmpl->param( SCALE => $scale );
	$tmpl->param( SIZE => $size );
	$tmpl->param( WIDTH => $width );
	$tmpl->param( HEIGHT => $height );
	$tmpl->param( MPM => $mpm );
	$tmpl->param( MODE => $template );
	$tmpl->param( XOFF => $xoff );
	$tmpl->param( YOFF => $yoff );

	# text
	$tmpl->param( TXT_SRC => $fromTxtSafe );
	$tmpl->param( TXT_DST => $toTxtSafe );
	$tmpl->param( TXT_ERROR => $ERROR );
	# helper text for searching
	$tmpl->param( SRC_HELP => $src_help );
	$tmpl->param( DST_HELP => $dst_help );

	$tmpl->param( LOCATION_OPT =>  $loc_opt);

	$tmpl->param( SRC_FOUND => $src_found );
	if($src_found){
		$tmpl->param( SRC_NAME => $locations->{'ByID'}{$from}{'Name'} );
		$tmpl->param( SRC_X => $locations->{'ByID'}{$from}{'x'} );
		$tmpl->param( SRC_Y => $locations->{'ByID'}{$from}{'y'} );
		#$tmpl->param( GOTO_SRC_URL => "javascript:centerOnLocation('src');" );
	}
	$tmpl->param( DST_FOUND => $dst_found );
	if($dst_found){
		$tmpl->param( DST_NAME => $locations->{'ByID'}{$to}{'Name'} );
		$tmpl->param( DST_X => $locations->{'ByID'}{$to}{'x'} );
		$tmpl->param( DST_Y => $locations->{'ByID'}{$to}{'y'} );
		#$tmpl->param( GOTO_DST_URL => "javascript:centerOnLocation('dst');" );
	}

	$tmpl->param( PATH_FOUND => $havePath );
	if($havePath){
		$tmpl->param( PATH_X => $pathImgRect->{'xmin'} );
		$tmpl->param( PATH_Y => $pathImgRect->{'ymin'} );
		$tmpl->param( PATH_W => $pathImgRect->{'xmax'} - $pathImgRect->{'xmin'} );
		$tmpl->param( PATH_H => $pathImgRect->{'ymax'} - $pathImgRect->{'ymin'} );
		$tmpl->param( PATH_DIST => $dist );
		$tmpl->param( PATH_SRC => $from );
		$tmpl->param( PATH_DST => $to );
	}

	# this is tells whether we're actually displaying a path between two separate locations
	$tmpl->param( DISTANCE => sprintf("%.02f", $dist || 0) );
	$tmpl->param( TIME => formatTime(($dist||0)*$mpm) );

	# a bunch of boolean values, for whatever strange logic we may need inside the template
	$tmpl->param( SRC_FOUND => $src_found );
	$tmpl->param( DST_FOUND => $dst_found );

	# helper text for searching
	$tmpl->param( SRC_HELP => $src_help );
	$tmpl->param( DST_HELP => $dst_help );
	#$tmpl->param( SRC_OR_DST_FOUND => ($src_found || $dst_found) );
	#$tmpl->param( SRC_AND_DST_FOUND => ($src_found && $dst_found) );
	#$tmpl->param( SRC_AND_DST_BLANK => ($fromTxt eq '' && $toTxt eq '') );

	# hex triplets representing the colors for the source and destination locations
	#$tmpl->param( SRC_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::SRC_COLOR));
	#$tmpl->param( DST_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::DST_COLOR));
	#$tmpl->param( PATH_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::PATH_COLOR));
	#$tmpl->param( BG_COLOR_HEX => sprintf("#%02x%02x%02x", @MapGlobals::LOC_BG_COLOR));

}
# XXX: theoretical print view?
#elsif ($template eq 'print'){
#
#}

# -----------------------------------------------------------------
# Everything is finally sent to the browser
# -----------------------------------------------------------------

print "Content-type: text/html\n\n" . $tmpl->output();

# that's all, folks!
