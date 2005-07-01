#!/usr/bin/perl -T
# -----------------------------------------------------------------
# map.cgi -- The user interface for the UCSDMap.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# TODO: Create some kind of 'viewport' object to avoid passing around
# these huge, indecipherable (and frequently-changing!) lists.
#
# Thu Jun 30 21:59:27 PDT 2005
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

use MapGlobals qw(TRUE FALSE INFINITY between asInt @SIZES @SCALES);
use LoadData;
use MapGraphics;
use ShortestPath;

my $q = new CGI();

# the name of this script, for form actions, etc
our $self = 'map.cgi';

# how far (in pixels) to pan when the user clicks "left", "right", "up", or
# "down"
my $pan = 100;

# generate a temporary file on disk to store the map image
my $tmpfile = new File::Temp(
		TEMPLATE => 'ucsdmap-XXXXXX',
		DIR => $MapGlobals::DYNAMIC_IMG_DIR,
		SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
		UNLINK => 0,
	);
chmod(0644, $tmpfile->filename);

# source and destination location names
my $fromTxt = $q->param('from') || shift(@ARGV) || '';
my $toTxt   = $q->param('to')   || shift(@ARGV) || '';

# we always need all the locations, so load them off disk
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);

# these may or may not be loaded later on
my($points, $edgeFH, $edgeSize);

# HTML-safe versions of the from and to text
my $fromTxtSafe = CGI::escapeHTML($fromTxt);
my $toTxtSafe = CGI::escapeHTML($toTxt);

# normalized versions of the from and to text
my $fromTxtLookup = LoadData::nameLookup($fromTxt);
my $toTxtLookup = LoadData::nameLookup($toTxt);

# x and y display offsets, from the upper left
my $xoff   = asInt($q->param('xoff')   || 0);
my $yoff   = asInt($q->param('yoff')   || 0);
# the zoom level of the map; this is an index into the MapGlobals::SCALES
# array, which contains the actual scale multipliers
my $scale  = asInt($q->param('scale')) || undef;	# this generates a warning, but we don't care
# the size of the map display;  this is an index into MapGlobals::SIZES
my $size   = asInt(defined($q->param('size')) ? $q->param('size') : 1);

# width and height of the viewing window (total image size is in MapGlobals)
my $width  = $SIZES[$size][0];
my $height = $SIZES[$size][1];

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

# kill old temporary files
MapGlobals::reaper($MapGlobals::DYNAMIC_MAX_AGE, $MapGlobals::DYNAMIC_IMG_SUFFIX);

# whether source and destination locations were found
my ($src_found, $dst_found) = (TRUE, TRUE);
# any text associated with the operation of finding the source or destination
# (this is "did you mean...?" stuff)
my($src_help, $dst_help) = ('', '');

# now convert $fromTxt and $toTxt to IDs
my $ERROR = '';
my ($from, $to) = (0, 0);
my @fromids = ();
my @toids = ();

# if we got no input, put up an informative message
if($fromTxt eq '' && $toTxt eq ''){
	#$ERROR .= "Enter a start and destination location to find the shortest"
	#	. " path between the two, or select only one to zoom to that location.";
	($src_found, $dst_found) = (FALSE, FALSE);

}
# otherwise, attempt to look up both locations
else{

	if( exists($locations->{$toTxtLookup}) ){
		$to = $locations->{$toTxtLookup}{'ID'};
	}
	elsif($toTxt ne ''){
		# fall back to fuzzy matching here
		@toids = LoadData::findName($toTxt, $locations);
		# we found an exact match
		if(@toids == 1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$to = $toids[0]{'id'};
			$toTxt = $locations->{$to}{'Name'};
			$toTxtSafe = CGI::escapeHTML($toTxt);
			$toTxtLookup = LoadData::nameLookup($toTxt);
		}
		# we have multiple matches
		elsif(@toids > 1){
			$dst_found = FALSE;
		}
		# no matches
		else{
			# I've done all I can. He's dead, Jim.
			$ERROR .= "<p>Destination location &quot;$toTxtSafe&quot; not found.</p>\n"
				if($toTxt ne '');
			$dst_found = FALSE;
		}
	}
	else{
		$dst_found = FALSE;
	}

	if( exists($locations->{$fromTxtLookup}) ){
		$from = $locations->{$fromTxtLookup}{'ID'};
	}
	elsif($fromTxt ne ''){
		# fall back to fuzzy matching here
		@fromids = LoadData::findName($fromTxt, $locations);
		# we found an exact match
		if(@fromids == 1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$from = $fromids[0]{'id'};
			$fromTxt = $locations->{$from}{'Name'};
			$fromTxtSafe = CGI::escapeHTML($fromTxt);
			$fromTxtLookup = LoadData::nameLookup($fromTxt);
		}
		# we have multiple matches
		elsif(@fromids > 1){
			$src_found = FALSE;
		}
		# no matches
		else{
			$ERROR .= "<p>Start location &quot;$fromTxtSafe&quot; not found.</p>\n"
				if($fromTxt ne '');
			$src_found = FALSE;
		}
	}
	else{
		$src_found = FALSE;
	}

}

# build a list of printable location names
my @locParam;
my($name, $trunc);
my($loc_opt_from, $loc_opt_to);
foreach (sort keys %$locations){
	# add each unique name
	if( substr($_, 0, 5) eq 'name:' ){

		# truncate the name if it's too long
		$trunc = $name = $locations->{$_}{'Name'};
		if(length($name) > $MapGlobals::MAX_NAME_LEN){
			$trunc = substr($name, 0, $MapGlobals::MAX_NAME_LEN) . '...';
		}

		$loc_opt_from .= sprintf(qq{<option value="%s"%s>%s</option>\n},
			CGI::escapeHTML($name),
			($fromTxt eq $locations->{$_}{'Name'} ? ' selected="selected"' : ''),
			CGI::escapeHTML($trunc)
		);
		$loc_opt_to .= sprintf(qq{<option value="%s"%s>%s</option>\n},
			CGI::escapeHTML($name),
			($toTxt eq $locations->{$_}{'Name'} ? ' selected="selected"' : ''),
			CGI::escapeHTML($trunc)
		);
	}
}

# we actually run the shortest-path algorithm only if we found both
# the source and destination locations
my $path = ($src_found && $dst_found);

# URL-safe versions of the from and to text
my $fromTxtURL = CGI::escape($fromTxt);
my $toTxtURL = CGI::escape($toTxt);

# these store the _path_ IDs of the starting and ending point, which we
# need to actually find the shortest path
my $startID = 0;
my $endID = 0;

# if start and end locations are the same, don't draw
if( $from == $to ){
	$path = FALSE;
}
# otherwise, assign the start and end IDs to meaningful values
else{
	$startID = $locations->{$fromTxtLookup}{'PointID'};
	$endID = $locations->{$toTxtLookup}{'PointID'};
}

# do the shortest path stuff
my $dist = 0;
my $rect;
my $pathPoints;
if($path){

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
		LoadData::cacheReaper();
	}
	
	if(defined($dist)){
		$dist /= $MapGlobals::PIXELS_PER_UNIT;
	}
	else{
		$path = FALSE;
		$ERROR .= "These two locations are not connected. ";
	}

	if(!$xoff && !$yoff){
		$xoff = int(($rect->{'xmin'} + $rect->{'xmax'}) / 2);
		$yoff = int(($rect->{'ymin'} + $rect->{'ymax'}) / 2);
	}

	# now pick a zoom level, if one hasn't already been specified
	if(!defined($scale)){
		my $w = $rect->{'xmax'} - $rect->{'xmin'};
		my $h = $rect->{'ymax'} - $rect->{'ymin'};

		# find the first level that encompasses the entire rectangle
		$scale = -1; # we set a bogus scale so we can test for it later
		for my $i (0..$#SCALES){
			if($w < $width/$SCALES[$i] && $h < $height/$SCALES[$i]){
				# we know it's big enough. now, make sure
				# nothing's too close to the edges

				# x pixels on the actual output image
				# correspond to x/$SCALES[$i] pixels on the
				# base image (to counteract the scale
				# multiplier).

				# if any of the locations are too close to any edge,
				# reject this zoom level and move to the next one
				if(abs($xoff - $locations->{$from}{'x'}) > $width/(2*$SCALES[$i]) - 5){
					next;
				}
				if(abs($xoff - $locations->{$to}{'x'}) > $width/(2*$SCALES[$i]) - 5){
					next;
				}

				if(abs($yoff - $locations->{$from}{'y'}) > $height/(2*$SCALES[$i]) - 5){
					next;
				}
				if(abs($yoff - $locations->{$to}{'y'}) > $height/(2*$SCALES[$i]) - 5){
					next;
				}

				# if location names would ordinarily go off the
				# edge of the screen, drawLocation draws them
				# to the left instead

				# if we made it this far, this is a good scale!
				$scale = $i;
				last;
			}
		}

		# if $scale is still a bogus value, we couldn't find ANY zoom level
		# to accomodate the two locations! fall back to centering on the destination,
		# and zooming in a bit
		if($scale == -1){
			$scale = $MapGlobals::SINGLE_LOC_SCALE;
			$xoff = $locations->{$to}{'x'};
			$yoff = $locations->{$to}{'y'};
		}
	}
}
# if we don't have a full path, check if we have a single location,
# and center on that
else{
	# if we got multiple matches, build help text to disambiguate
	if(@fromids > 1){
		# if we found a good 'from' location, run shortest path stuff
		# so we can display distances
		if($dst_found){
			$points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
			ShortestPath::find($endID, $points);
		}
		$src_help = "<p><b>Start location &quot;$fromTxtSafe&quot; not found.</b></p>"
			. buildHelpText(undef, $toTxtURL, $mpm, $locations, $points, \@fromids);
	}
	if(@toids > 1){
		# if we found a good 'to' location, run shortest path stuff
		# so we can display distances
		if($src_found){
			$points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
			ShortestPath::find($startID, $points);
		}
		$dst_help = "<p><b>Destination location &quot;$toTxtSafe&quot; not found.</b></p>"
			. buildHelpText($fromTxtURL, undef, $mpm, $locations, $points, \@toids);
	}

	if($src_found){
		$xoff = $locations->{$from}{'x'};
		$yoff = $locations->{$from}{'y'};
	}
	elsif($dst_found){
		$xoff = $locations->{$to}{'x'};
		$yoff = $locations->{$to}{'y'};
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
$xoff = between(($width/$SCALES[$scale])/2, $MapGlobals::IMAGE_X - ($width/$SCALES[$scale])/2, $xoff);
$yoff = between(($height/$SCALES[$scale])/2, $MapGlobals::IMAGE_Y - ($height/$SCALES[$scale])/2, $yoff);

# adjust xoff/yoff so they point to the upper-left-hand corner, which is what
# the low-level MapGraphics functions use. additionally, take scale into
# account at the right time, so it doesn't offset scaled views.
my $rawxoff = int($xoff*$SCALES[$scale] - $width/2);
my $rawyoff = int($yoff*$SCALES[$scale] - $height/2);


# get the image of the appropriate scale from disk, and grab only
# what we need by size and offset
my $im = GD::Image->newFromGd2Part(MapGlobals::getGd2Filename($SCALES[$scale]),
	$rawxoff, $rawyoff, $width, $height)
	|| die "Could not load base image " . MapGlobals::getGd2Filename($SCALES[$scale]) . "\n";

my $src_color = $im->colorAllocate(@MapGlobals::SRC_COLOR);
my $dst_color = $im->colorAllocate(@MapGlobals::DST_COLOR);
my $path_color = $im->colorAllocate(@MapGlobals::PATH_COLOR);
my $bg_color = $im->colorAllocate(@MapGlobals::LOC_BG_COLOR);

# uncomment to draw ALL paths
#MapGraphics::drawAllEdges($edges, $im, 1, $path_color, $rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);

# uncomment to draw ALL locations
#MapGraphics::drawAllLocations($locations, $im, $src_color, $src_color, $bg_color, $rawxoff, $rawyoff,
#				$width, $height, $SCALES[$scale]);

# if we had a path to draw, now's the time to do it
if($path){
	foreach my $line (@$pathPoints){
		MapGraphics::drawLines($line, $im, 2, $path_color, $rawxoff, $rawyoff,
			$width, $height, $SCALES[$scale]);
	}
	# uncomment to draw bounding rectangle
	#$im->setThickness(1);
	#$im->rectangle(
	#	$rect->{'xmin'}*$SCALES[$scale] - $rawxoff,
	#	$rect->{'ymin'}*$SCALES[$scale] - $rawyoff,
	#	$rect->{'xmax'}*$SCALES[$scale] - $rawxoff,
	#	$rect->{'ymax'}*$SCALES[$scale] - $rawyoff,
	#	$rect_color
	#);
}

# only draw the source and destination locations
if($src_found){
	MapGraphics::drawLocation($locations->{$from}, $im, $src_color, $src_color, $bg_color,
		$rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);
}
if($dst_found){
	MapGraphics::drawLocation($locations->{$to}, $im, $dst_color, $dst_color, $bg_color,
		$rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);
}

# print the data out to a temporary file
binmode($tmpfile);
print $tmpfile $im->png();
close($tmpfile);


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
	($xoff - ($width/$SCALES[$scale])/2)*$MapGlobals::RATIO_X,
	($yoff - ($height/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y,
	($xoff + ($width/$SCALES[$scale])/2)*$MapGlobals::RATIO_X - 1,
	($yoff + ($height/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y - 1,
	$thumb_rect_color
);

# dots for the start and end locations
if($src_found){
	$thumb->filledRectangle(
		$locations->{$from}{'x'}*$MapGlobals::RATIO_X - 1,
		$locations->{$from}{'y'}*$MapGlobals::RATIO_Y - 1,
		$locations->{$from}{'x'}*$MapGlobals::RATIO_X + 1,
		$locations->{$from}{'y'}*$MapGlobals::RATIO_Y + 1,
		$thumb_src_color,
	);
}

if($dst_found){
	$thumb->filledRectangle(
		$locations->{$to}{'x'}*$MapGlobals::RATIO_X - 1,
		$locations->{$to}{'y'}*$MapGlobals::RATIO_Y - 1,
		$locations->{$to}{'x'}*$MapGlobals::RATIO_X + 1,
		$locations->{$to}{'y'}*$MapGlobals::RATIO_Y + 1,
		$thumb_dst_color,
	);
}

# now make a temporary file to put this image in
# XXX: eventually just make this a separate CGI script?
# I don't know -- which has higher cost: creating/deleting a file, or starting
# another perl process?
my $tmpthumb = new File::Temp(
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

# states for the directions
my $left  = state($fromTxtURL, $toTxtURL, $xoff - $pan/$SCALES[$scale], $yoff, $scale, $size, $mpm);
my $right = state($fromTxtURL, $toTxtURL, $xoff + $pan/$SCALES[$scale], $yoff, $scale, $size, $mpm);
my $up    = state($fromTxtURL, $toTxtURL, $xoff, $yoff - $pan/$SCALES[$scale], $scale, $size, $mpm);
my $down  = state($fromTxtURL, $toTxtURL, $xoff, $yoff + $pan/$SCALES[$scale], $scale, $size, $mpm);

# the diagonal buttons actually cheat: the total movement is about 141 pixels,
# because we move 100 up AND 100 left, for instance. I don't think anyone cares.
# XXX: unused at the moment -- not sure how to fit them into the display cleanly
#my $upLeft    = state($fromTxtURL, $toTxtURL,
#	$xoff - $pan/$SCALES[$scale], $yoff - $pan/$SCALES[$scale], $scale, $size, $mpm);
#my $upRight   = state($fromTxtURL, $toTxtURL,
#	$xoff + $pan/$SCALES[$scale], $yoff - $pan/$SCALES[$scale], $scale, $size, $mpm);
#my $downLeft  = state($fromTxtURL, $toTxtURL,
#	$xoff - $pan/$SCALES[$scale], $yoff + $pan/$SCALES[$scale], $scale, $size, $mpm);
#my $downRight = state($fromTxtURL, $toTxtURL,
#	$xoff + $pan/$SCALES[$scale], $yoff + $pan/$SCALES[$scale], $scale, $size, $mpm);

# buttons to make the window bigger/smaller 
my $bigger = state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size < $#SIZES) ? $size+1 : $size, $mpm);
my $smaller = state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size > 0) ? $size-1 : $size, $mpm);

# make sure the offsets don't go out of range
$rawxoff = between(0, $MapGlobals::IMAGE_X - $width, $rawxoff);
$rawyoff = between(0, $MapGlobals::IMAGE_Y - $height, $rawyoff);

# now we slam everything into a template and print it out
my $tmpl = HTML::Template->new(
	filename => $MapGlobals::TEMPLATE,
	die_on_bad_params => 0,
	global_vars => 1
);

# basic info: who we are, where to find images, etc
$tmpl->param( SELF => $self ); # whoooooooooo are you?

# a bunch of CVS tags
#$tmpl->param( CVS_ID => '$Id$');
$tmpl->param( CVS_REVISION => '$Revision$');
#$tmpl->param( CVS_DATE => '$Date$');
#$tmpl->param( CVS_AUTHOR => '$Author$');

# URLS and pathnames to various important things
# (Note: IMG_* tags are only used by the old template.)
#$tmpl->param( IMG_UP => "$STATIC_IMG_DIR/up.png" );
#$tmpl->param( IMG_DOWN => "$STATIC_IMG_DIR/down.png" );
#$tmpl->param( IMG_LEFT => "$STATIC_IMG_DIR/left.png" );
#$tmpl->param( IMG_RIGHT => "$STATIC_IMG_DIR/right.png" );

$tmpl->param( IMG_VIEW => $tmpfile->filename );
$tmpl->param( IMG_THUMB => $tmpthumb->filename );
$tmpl->param( IMG_DIR => $MapGlobals::STATIC_IMG_DIR );

# add info about current state
$tmpl->param( SCALE => $scale );
$tmpl->param( SIZE => $size );
$tmpl->param( MPM => $mpm );
$tmpl->param( XOFF => $xoff );
$tmpl->param( YOFF => $yoff );
#$tmpl->param( VIEW_WIDTH => $width );
#$tmpl->param( VIEW_HEIGHT => $height );
#$tmpl->param( THUMB_WIDTH => $MapGlobals::THUMB_X );
#$tmpl->param( THUMB_HEIGHT => $MapGlobals::THUMB_Y );

$tmpl->param( ZOOM_WIDGET =>
	listZoomLevels($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, $size));

#$tmpl->param( LOCATIONS => \@locParam );
$tmpl->param( LOCATION_OPT_FROM => $loc_opt_from);
$tmpl->param( LOCATION_OPT_TO =>  $loc_opt_to);

# the strings representing the state of various buttons
$tmpl->param( UP_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff - $pan/$SCALES[$scale], $scale, $size, $mpm));
$tmpl->param( DOWN_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff + $pan/$SCALES[$scale], $scale, $size, $mpm));
$tmpl->param( LEFT_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff - $pan/$SCALES[$scale], $yoff, $scale, $size, $mpm));
$tmpl->param( RIGHT_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff + $pan/$SCALES[$scale], $yoff, $scale, $size, $mpm));
#$tmpl->param( SMALLER_URL => 
#	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size > 0) ? $size-1 : $size, $mpm));
#$tmpl->param( BIGGER_URL => 
#	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size < $#SIZES) ? $size+1 : $size, $mpm));

$tmpl->param( ZOOM_OUT_URL => "$self?" . state($fromTxtSafe, $toTxtSafe, $xoff, $yoff,
	($scale < $#MapGlobals::SCALES) ? $scale + 1 : $#MapGlobals::SCALES, $size, $mpm));
$tmpl->param( ZOOM_IN_URL => "$self?" . state($fromTxtSafe, $toTxtSafe, $xoff, $yoff,
	($scale > 0) ? $scale - 1 : 0, $size, $mpm));

$tmpl->param( RECENTER_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, undef, undef, undef, undef, $mpm));

# text
$tmpl->param( TXT_SRC => $fromTxtSafe );
$tmpl->param( TXT_DST => $toTxtSafe );
$tmpl->param( TXT_ERROR => $ERROR );

# this is tells whether we're actually displaying a path between two separate locations
$tmpl->param( GOT_PATH => $path );
$tmpl->param( DISTANCE => sprintf("%.02f", $dist) );
$tmpl->param( TIME => sprintf("%.02f", $dist*$mpm) );

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

print "Content-type: text/html\n\n" . $tmpl->output();

###################################################################
# Given information about the current map state, return a query string that
# will preserve the given state.
# Args:
#	- source location, as text
#	- destination location, as text
#	- x-offset
#	- y-offset
#	- map scale
#	- viewing window size
#	- minutes it takes the user to walk one mile
# Returns:
#	- query string
###################################################################
sub state{
	my ($from, $to, $x, $y, $scale, $size, $mpm) = (@_);
	my @keys = qw(from to xoff yoff scale size mpm);
	my $str;
	for my $i (0..$#_){
		if(defined($_[$i])){
			$str .= '&amp;' if defined($str);
			$str .= "$keys[$i]=$_[$i]";
		}
	}
	return $str;
}

###################################################################
# Return a list of zoom levels that can be used to build a zoom widget inside
# the template.
# Args:
#	Identical to state().
# Returns:
#	- an array of hashrefs containing info for each zoom level
###################################################################
sub listZoomLevels{
	my ($from, $to, $x, $y, $scale, $size, $mpm) = (@_);

	my @ret;
	for my $i (0..$#MapGlobals::SCALES){
		push(@ret, {
			URL => $self . '?' . state($from, $to, $x, $y, $i, $size, $mpm),
			SELECTED => ($i == $scale),
			LEVEL => $i,
		});
	}

	return \@ret;
}

###################################################################
# Given EITHER the 'from' search text or the 'to' search text (there are two
# arguments, but one should be undefined), some script state, and a list of IDs
# of suggested locations, build and return HTML for a list that shows the
# suggested locations.
#
# If given a list of GraphPoints run through ShortestPath::find as well, each
# location's distance from the source location (determined by find()) will also
# be listed.
#
# This is for when a search has failed, and a number of alternate matches have
# been found.
#
# Args:
#	- 'from' search text (or undef)
#	- 'to' search text (or undef)
#	- miles per minute (to perserve state)
#	- hashref of locations
#	- hashref of GraphPoints (optional, to list distances)
#	- arrayref of location IDs (ints) that are possible matches to the
#	  search term
###################################################################
sub buildHelpText{
	my ($fromTxt, $toTxt, $mpm, $locations, $points, $ids) = (@_);

	# figure out which kind of help we're giving
	# one of $fromTxt and $toTxt will be undef. whichever one is,
	# that's the one we have a list of IDs for.
	my $helpfor = defined($fromTxt) ? 'to' : 'from';

	#my $str = "<p><b>Found " . @$ids . " close matches:</b></p>\n<ol>\n";
	my $str = "<p><b>Did you mean...</b></p>\n<ol>\n";
	my $url;
	foreach (@$ids){
		my $dist_txt = '';
		if( defined($points) ){
			my $target = $points->{$locations->{$_->{'id'}}{'PointID'}};
			if($target->{'Distance'} == INFINITY){
				$dist_txt = ' (&#8734;)'; # this is the infinity symbol
			}
			elsif($target->{'Distance'} == 0){
				$dist_txt = '';
			}
			else{
				$dist_txt = sprintf(" (%0.2f mi)",
					ShortestPath::distTo($points, $target)/$MapGlobals::PIXELS_PER_UNIT);
			}
		}

		# we need to avoid flipping the 'from' and 'to' locations here, so
		# we're careful about which location we're actually searching for
		if($helpfor eq 'from'){
			$url = state(CGI::escape($locations->{$_->{'id'}}{'Name'}), $toTxt,
				undef, undef, undef, undef, $mpm);
		}
		else{
			$url = state($fromTxt, CGI::escape($locations->{$_->{'id'}}{'Name'}),
				undef, undef, undef, undef, $mpm);
		}
		$str .= sprintf(qq|\t<li><a href="$self?%s">%s</a>%s</li>\n|,
			$url, CGI::escapeHTML($locations->{$_->{'id'}}{'Name'}), $dist_txt);
	}
	$str .= "</ol>\n";

	return $str;
}
