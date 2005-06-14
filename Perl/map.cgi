#!/usr/bin/perl
# -----------------------------------------------------------------
# map.cgi -- The user interface for the UCSDMap.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# TODO: Create some kind of 'state' object to avoid passing around
# these huge, indecipherable (and frequently-changing!) lists.
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Sun Jun 12 13:22:05 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use CGI;
use File::Temp ();
use HTML::Template;

use MapGlobals
	qw(@SCALES INFINITY TRUE FALSE $DYNAMIC_IMG_DIR $STATIC_IMG_DIR between);
use LoadData;
use MapGraphics;
use ShortestPath;

use GD::Polyline;

my $q = CGI::new();

# window sizes
my @sizes = (
	[400, 300],
	[500, 375],
	[640, 480],
);

# the name of this script, for form actions, etc
our $self = 'map.cgi';

# how far (in pixels) to pan when the user clicks "left", "right", "up", or
# "down"
my $pan = 100;

# generate a dynamic filename
# (this can be used as a string to get its filename, toString()-style)
my $tmpfile = new File::Temp(
		TEMPLATE => 'ucsdmap-XXXXXX',
		DIR => $DYNAMIC_IMG_DIR,
		SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
		UNLINK => 0,
	);
chmod(0644, $tmpfile->filename);

# load all the data we'll need
my $points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
my $locations	= LoadData::loadLocations($MapGlobals::LOCATION_FILE);
my $edges	= LoadData::loadEdges($MapGlobals::EDGE_FILE);

# source and destination location names
my $fromTxt = $q->param('from') || '';
my $toTxt   = $q->param('to')   || '';

# HTML-safe versions of the from and to text
my $fromTxtSafe = CGI::escapeHTML($fromTxt);
my $toTxtSafe = CGI::escapeHTML($toTxt);

# normalized versions of the from and to text
my $fromTxtLookup = LoadData::nameLookup($fromTxt);
my $toTxtLookup = LoadData::nameLookup($toTxt);

# x and y display offsets, from the upper left
my $xoff   = int($q->param('xoff')   || 0);
my $yoff   = int($q->param('yoff')   || 0);
my $scale  =     $q->param('scale');
my $size   = int(defined($q->param('size')) ? $q->param('size') : 1);

# how fast do you walk?
# in minutes per mile.
my $mpm  =     $q->param('mpm')  || 15;

# width and height of the viewing window (total image size is in MapGlobals)
my $width  = $sizes[$size][0];
my $height = $sizes[$size][1];

# click offsets from the map
my $mapx = defined($q->param('map.x')) ? int($q->param('map.x')) : undef;
my $mapy = defined($q->param('map.y')) ? int($q->param('map.y')) : undef;

# click offsets from the thumbnail
my $thumbx = defined($q->param('thumb.x')) ? int($q->param('thumb.x')) : undef;
my $thumby = defined($q->param('thumb.y')) ? int($q->param('thumb.y')) : undef;
# one or the other (map or thumb click offsets) should be undef

# clear out any old images
MapGlobals::reaper($MapGlobals::DYNAMIC_MAX_AGE, $MapGlobals::DYNAMIC_IMG_SUFFIX);

# whether source and destination locations were found
my ($src_found, $dst_found) = (TRUE, TRUE);

# now convert $fromTxt and $toTxt to IDs
my $ERROR = '';
my $DEBUG = '';
my ($from, $to) = (0, 0);

# if we got no input, put up an informative message
if($fromTxt eq '' && $toTxt eq ''){
	$ERROR .= "Enter a start and destination location to find the shortest"
		. " path between the two, or select only one to zoom to that location.";
	($src_found, $dst_found) = (FALSE, FALSE);

}
# otherwise, attempt to look up both locations
else{
	# the ordering of these two if-blocks is significant!
	# switching them results in the view centering on the START location.

	if( exists($locations->{$toTxtLookup}) ){
		$to = $locations->{$toTxtLookup}{'ID'};
	}
	elsif($toTxt ne ''){
		# fall back to fuzzy matching here
		my $id = LoadData::findName($toTxt, $locations);
		if($id != -1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$to = $id;
			$toTxt = $locations->{$id}{'Name'};
			$toTxtSafe = CGI::escapeHTML($toTxt);
			$toTxtLookup = LoadData::nameLookup($toTxt);
		}
		else{
			# I've done all I can. He's dead, Jim.
			$ERROR .= "<b>Destination location &quot;$toTxtSafe&quot; not found.</b>\n"
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
		my $id = LoadData::findName($fromTxt, $locations);
		if($id != -1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$from = $id;
			$fromTxt = $locations->{$id}{'Name'};
			$fromTxtSafe = CGI::escapeHTML($fromTxt);
			$fromTxtLookup = LoadData::nameLookup($fromTxt);
		}
		else{
			$ERROR .= "<b>Start location &quot;$fromTxtSafe&quot; not found.</b>\n"
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
foreach (sort keys %$locations){
	# add each unique name
	if( substr($_, 0, 5) eq 'name:' ){
		push(@locParam, {
			NAME => CGI::escapeHTML($locations->{$_}{'Name'}),
			SELECTED_TO => ($toTxt eq $locations->{$_}{'Name'}),
			SELECTED_FROM => ($fromTxt eq $locations->{$_}{'Name'}),
		});
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
my $dist;
my $rect;
my $pathPoints;
if($path){
	ShortestPath::find($startID, $points);

	($dist, $rect, $pathPoints) = ShortestPath::pathPoints($points, $edges, $points->{$endID});

	$dist /= $MapGlobals::PIXELS_PER_UNIT;

	if(!$xoff && !$yoff){
		$xoff = int(($rect->{'xmin'} + $rect->{'xmax'}) / 2);
		$yoff = int(($rect->{'ymin'} + $rect->{'ymax'}) / 2);
	}

	# now pick a zoom level, if one hasn't already been specified
	if(!defined($scale)){
		my $w = $rect->{'xmax'} - $rect->{'xmin'};
		my $h = $rect->{'ymax'} - $rect->{'ymin'};

		# find the first level that encompasses the entire rectangle
		for my $i (0..$#SCALES){
			if($w < $width/$SCALES[$i] && $h < $height/$SCALES[$i]){
				$scale = $i;
				last;
			}
		}
	}
}
# if we don't have a full path, check if we have a single location,
# and center on that
else{
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
		$scale = $MapGlobals::DEFAULT_SCALE;
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

my $im = GD::Image->newFromGd2Part(MapGlobals::getGd2Filename($SCALES[$scale]),
	$rawxoff, $rawyoff, $width, $height)
	|| die "Could not load image $MapGlobals::BASE_GD2_IMAGE\n";

my $red = $im->colorAllocate(255, 0, 0);
my $green = $im->colorAllocate(0, 255, 0);

# uncomment to draw ALL paths
#MapGraphics::drawAllEdges($edges, $im, 1, $red, $rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);

# uncomment to draw ALL locations
#MapGraphics::drawAllLocations($locations, $im, $red, $red, $rawxoff, $rawyoff,
#				$width, $height, $SCALES[$scale]);

if($path){
	foreach my $line (@$pathPoints){
		MapGraphics::drawLines($line, $im, 2, $green, $rawxoff, $rawyoff,
			$width, $height, $SCALES[$scale]);
	}

	# uncomment to draw bounding rectangle
	#$im->setThickness(1);
	#$im->rectangle(
	#	$rect->{'xmin'}*$SCALES[$scale] - $rawxoff,
	#	$rect->{'ymin'}*$SCALES[$scale] - $rawyoff,
	#	$rect->{'xmax'}*$SCALES[$scale] - $rawxoff,
	#	$rect->{'ymax'}*$SCALES[$scale] - $rawyoff,
	#	$red
	#);
}

# only draw the source and destination locations
if($src_found){
	MapGraphics::drawLocation($locations->{$from}, $im, $red, $red, $rawxoff, $rawyoff,
					$width, $height, $SCALES[$scale]);
}
if($dst_found){
	MapGraphics::drawLocation($locations->{$to}, $im, $red, $red, $rawxoff, $rawyoff,
					$width, $height, $SCALES[$scale]);
}

# print the data out to a temporary file
binmode($tmpfile);
print $tmpfile $im->png();
close($tmpfile);


# now that we have valid offsets, we can generate the thumbnail (highlighting
# the visible window) safely
my $thumb = GD::Image->newFromPng($MapGlobals::THUMB_FILE, 1);

# store the ratio between the thumbnail and the main base image
# (these two REALLY should be the same...)
my $ratio_x = $MapGlobals::THUMB_X / $MapGlobals::IMAGE_X;
my $ratio_y = $MapGlobals::THUMB_Y / $MapGlobals::IMAGE_Y;

# this is the color in which we draw the edge-of-view lines
my $yellow = $thumb->colorAllocate(0, 230, 230);

# top line
$thumb->rectangle(
	($xoff - ($width/$SCALES[$scale])/2)*$MapGlobals::RATIO_X,
	($yoff - ($height/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y,
	($xoff + ($width/$SCALES[$scale])/2)*$MapGlobals::RATIO_X - 1,
	($yoff + ($height/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y - 1,
	$yellow
);

# now make a temporary file to put this image in
# XXX: eventually just make this a separate CGI script?
# I don't know -- which has higher cost: creating/deleting a file, or starting
# another perl process?
my $tmpthumb = new File::Temp(
		TEMPLATE => 'ucsdmap-XXXXXX',
		DIR => $DYNAMIC_IMG_DIR,
		SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
		UNLINK => 0,
	);
chmod(0644, $tmpthumb->filename);

# write out the finished thumbnail to the file
binmode($tmpthumb);
print $tmpthumb $thumb->png();
close($tmpthumb);

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
my $bigger = state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size < $#sizes) ? $size+1 : $size, $mpm);
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
$tmpl->param( CVS_ID => '$Id$');
$tmpl->param( CVS_REVISION => '$Revision$');
$tmpl->param( CVS_DATE => '$Date$');
$tmpl->param( CVS_AUTHOR => '$Author$');

# URLS and pathnames to various important things
# (Note: IMG_* tags are only used by the old template.)
$tmpl->param( IMG_UP => "$STATIC_IMG_DIR/up.png" );
$tmpl->param( IMG_DOWN => "$STATIC_IMG_DIR/down.png" );
$tmpl->param( IMG_LEFT => "$STATIC_IMG_DIR/left.png" );
$tmpl->param( IMG_RIGHT => "$STATIC_IMG_DIR/right.png" );

$tmpl->param( IMG_VIEW => $tmpfile->filename );
$tmpl->param( IMG_THUMB => $tmpthumb->filename );
$tmpl->param( IMG_DIR => $STATIC_IMG_DIR );

# add info about current state
$tmpl->param( SCALE => $scale );
$tmpl->param( SIZE => $size );
$tmpl->param( MPM => $mpm );
$tmpl->param( XOFF => $xoff );
$tmpl->param( YOFF => $yoff );
$tmpl->param( VIEW_WIDTH => $width );
$tmpl->param( VIEW_HEIGHT => $height );
$tmpl->param( THUMB_WIDTH => $MapGlobals::THUMB_X );
$tmpl->param( THUMB_HEIGHT => $MapGlobals::THUMB_Y );

$tmpl->param( ZOOM_WIDGET =>
	listZoomLevels($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, $size));

$tmpl->param( LOCATIONS => \@locParam );

# the strings representing the state of various buttons
$tmpl->param( UP_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff - $pan/$SCALES[$scale], $scale, $size, $mpm));
$tmpl->param( DOWN_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff + $pan/$SCALES[$scale], $scale, $size, $mpm));
$tmpl->param( LEFT_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff - $pan/$SCALES[$scale], $yoff, $scale, $size, $mpm));
$tmpl->param( RIGHT_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff + $pan/$SCALES[$scale], $yoff, $scale, $size, $mpm));
$tmpl->param( SMALLER_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size > 0) ? $size-1 : $size, $mpm));
$tmpl->param( BIGGER_URL => 
	"$self?" . state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size < $#sizes) ? $size+1 : $size, $mpm));

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

$tmpl->param( SRC_FOUND => $src_found );
$tmpl->param( DST_FOUND => $dst_found );

# this is for shuttling various debug messages out to the interface
$tmpl->param( DEBUG => $DEBUG );

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
