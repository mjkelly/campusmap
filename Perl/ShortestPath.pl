#!/usr/bin/perl
# -----------------------------------------------------------------
# ShortestPath.pl -- Find the shortest path between two vertices,
# using Dijkstra's algorithm.
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Thu Mar 24 21:34:09 PST 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use LoadData;
use MapGlobals;
use MapGraphics;
use ShortestPath;

use GD;

my $points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
my $locations	= LoadData::loadLocations($MapGlobals::LOCATION_FILE);
my $edges	= LoadData::loadEdges($MapGlobals::EDGE_FILE);

my $im = GD::Image->newFromPng($MapGlobals::BASE_IMAGE)
	|| die "Could not load image $MapGlobals::BASE_IMAGE\n";

my $red = $im->colorAllocate(255, 0, 0);
my $green = $im->colorAllocate(0, 255, 0);

# set these to modify the points highlighted
my $startID = 3;
my $endID = 4;

foreach my $pointID ( sort(keys(%$points)) ){
	# print this point's fields
	print "Point ID: $points->{$pointID}{'ID'}\n";
	print "Distance: " . (($points->{$pointID}{'Distance'} == INFINITY)
		? 'inf' : $points->{$pointID}{'Distance'}) . "\n";
	print "From ID: " . (defined($points->{$pointID}{'From'}{'ID'})
		? $points->{$pointID}{'From'}{'ID'} : 'undef') . "\n";
}

ShortestPath::find($startID, $points);

foreach my $pointID ( sort(keys(%$points)) ){
	# print this point's fields
	print "Point ID: $points->{$pointID}{'ID'}\n";
	print "Distance: " . (($points->{$pointID}{'Distance'} == INFINITY)
		? 'inf' : $points->{$pointID}{'Distance'}) . "\n";
	print "From ID: " . (defined($points->{$pointID}{'From'}{'ID'})
		? $points->{$pointID}{'From'}{'ID'} : 'undef') . "\n";
	print "\n";
}

print "SHORTEST PATH:\n";
print ShortestPath::pathTo($points, $points->{$endID}) . "\n";

# print the background of the graph
MapGraphics::drawAllEdges($edges, $im, 1, $red);
MapGraphics::drawAllLocations($locations, $im, $red, $red);

# print the path we took
ShortestPath::drawTo($points, $edges, $points->{$endID}, $im, $green);

open(OFH, '>', $MapGlobals::OUT_IMAGE) || die "Cannot open output file for reading: $!\n";
binmode(OFH);
print OFH $im->png();
close(OFH);

