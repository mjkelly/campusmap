#!/usr/bin/perl
# -----------------------------------------------------------------
# shortest.cgi -- $desc$
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Fri Mar 25 01:51:45 PST 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use MapGlobals;
use MapGraphics;
use LoadData;
use ShortestPath;

use CGI;

my $q = CGI::new();


my $points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
my $locations	= LoadData::loadLocations($MapGlobals::LOCATION_FILE);
my $edges	= LoadData::loadEdges($MapGlobals::EDGE_FILE);

print "Content-type: image/png\n\n";

# input parameters
my $from   = int($q->param('from')   || 0);
my $to     = int($q->param('to')     || 0);
my $width  = int($q->param('width')  || 500);
my $height = int($q->param('height') || 400);
my $xoff   = int($q->param('xoff')   || 0);
my $yoff   = int($q->param('yoff')   || 0);

my $path = TRUE;

# these store the _path_ IDs of the starting and ending point, which we
# need to actually find the shortest path
my $startID = 0;
my $endID = 0;

# normalize the from and to fields so case, spaces, etc, don't mess us up
$from = LoadData::nameNormalize($from);
$to = LoadData::nameNormalize($to);

# if start and end locations are the same, don't draw
if( $from == $to ){
	$path = FALSE;

	# set the offsets so we center on the one location that was selected
	$xoff += $locations->{$to}{'x'} - $width/2;
	$yoff += $locations->{$to}{'y'} - $height/2;
}
# if either location doesn't exist, don't draw
elsif( !exists($locations->{$from}) || !exists($locations->{$to}) ){
	$path = FALSE;
}
# otherwise, assign the start and end IDs to meaningful values
else{
	$startID = $locations->{$from}{'PointID'};
	$endID = $locations->{$to}{'PointID'};

	# add in the offset of the destination
	$xoff += $locations->{$to}{'x'} - $width/2;
	$yoff += $locations->{$to}{'y'} - $height/2;
}

# make sure the offsets don't go out of range
$xoff = between(0, $MapGlobals::IMAGE_X - $width, $xoff);
$yoff = between(0, $MapGlobals::IMAGE_Y - $height, $yoff);

my $im = GD::Image->newFromGd2Part($MapGlobals::BASE_GD2_IMAGE, $xoff, $yoff, $width, $height)
	|| die "Could not load image $MapGlobals::BASE_GD2_IMAGE\n";

my $red = $im->colorAllocate(255, 0, 0);
my $green = $im->colorAllocate(0, 255, 0);

#MapGraphics::drawAllEdges($edges, $im, 1, $red, $xoff, $yoff, $width, $height);
MapGraphics::drawAllLocations($locations, $im, $red, $red, $xoff, $yoff, $width, $height);

# do the shortest path stuff
if($path){
	ShortestPath::find($startID, $points);
	ShortestPath::drawTo($points, $edges, $points->{$endID}, $im, $green,
		$xoff, $yoff, $width, $height);
}

binmode(STDOUT);
print STDOUT $im->png();

# given A, B, and C, returns a value D that is as close to C as possible while
# still being in [A, B].
sub between{
	my($min, $max, $val) = (@_);
	if($val < $min){
		return $min;
	}
	if($val > $max){
		return $max;
	}
	return $val;
}
