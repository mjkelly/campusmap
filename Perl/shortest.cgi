#!/usr/bin/perl
# -----------------------------------------------------------------
# $Id$
# shortest.cgi -- A quick CGI script that find the shortest
# path between the two named locations.
# Copyright 2005 David Lindquist and Michael Kelly
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

my $from = $q->param('from');
my $to = $q->param('to');
#my $from = 'start';
#my $to = 'end';

my $startID = $locations->{"name:$from"}{'PointID'};
my $endID = $locations->{"name:$to"}{'PointID'};

#print "From: $from ($startID)\n";
#print "To: $to ($endID)\n";


my $im = GD::Image->newFromPng($MapGlobals::BASE_IMAGE)
	|| die "Could not load image $MapGlobals::BASE_IMAGE\n";

my $red = $im->colorAllocate(255, 0, 0);
my $green = $im->colorAllocate(0, 255, 0);

ShortestPath::find($startID, $points);

MapGraphics::drawAllEdges($edges, $im, 1, $red);
MapGraphics::drawAllLocations($locations, $im, $red, $red);

# print the path we took
ShortestPath::drawTo($points, $edges, $points->{$endID}, $im, $green);

#open(OFH, '>', $MapGlobals::OUT_IMAGE) || die "Cannot open output file for reading: $!\n";
binmode(STDOUT);
print STDOUT $im->png();
#close(OFH);
