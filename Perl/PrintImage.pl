#!/usr/bin/perl
# -----------------------------------------------------------------
# $Id$
# PrintImage.pl -- Load binary data written from PathOptimize
# and write it onto a map image.
# Copyright 2005 David Lindquist and Michael Kelly
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Thu Mar 24 18:10:41 PST 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use LoadData;
use MapGlobals;
use MapGraphics;

use GD;

my $points		= LoadData::loadPoints($MapGlobals::POINT_FILE);
my $locations	= LoadData::loadLocations($MapGlobals::LOCATION_FILE);
my $edges		= LoadData::loadEdges($MapGlobals::EDGE_FILE);

my $im = GD::Image->newFromPng($MapGlobals::BASE_IMAGE)
	|| die "Could not load image $MapGlobals::BASE_IMAGE\n";


my $lineColor = $im->colorAllocate(255, 0, 0);
my $labelColor = $im->colorAllocate(0, 255, 0);

MapGraphics::drawAllEdges($edges, $im, 1, $lineColor);

MapGraphics::drawAllLocations($locations, $im, $labelColor, $lineColor);

open(OFH, '>', $MapGlobals::OUT_IMAGE) || die "Cannot open output file for reading: $!\n";
binmode(OFH);
print OFH $im->png();
close(OFH);

