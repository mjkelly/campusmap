#!/usr/bin/perl
# -----------------------------------------------------------------
# $Id$
# MapGraphics.pl -- Functions for manipulating the map image.
# Copyright 2005 David Lindquist and Michael Kelly
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Fri Mar 25 00:39:45 PST 2005
# -----------------------------------------------------------------

package MapGraphics;

use strict;
use warnings;

use GD;

###################################################################
# Draw the given edge to a GD image in the given color.
# Args:
#	- a reference to the edge to print
#	- the GD image to print to
#	- the thickness of the line to print, in pixels
#	- the GD color object to use
# Returns:
#	- nothing
###################################################################
sub drawEdge{
	my($edge, $im, $thickness, $color) = (@_);

	# draw all the Edge lines
	my($curpt, $prevpt);

	$im->setThickness($thickness);

	# cycle through each point in this Edge
	foreach $curpt ( @{$edge->{'Path'}} ){
		if( defined($prevpt) ){
			#print "($prevpt->{'x'}, $prevpt->{'y'})"
			#	. "-($curpt->{'x'}, $curpt->{'y'})\n";

			# draw a line between the previous point and the current point
			$im->line($prevpt->{'x'}, $prevpt->{'y'},
				$curpt->{'x'}, $curpt->{'y'}, $color);

		}
		$prevpt = $curpt;
	}

}

sub drawAllEdges{
	my($edges, $im, $thickness, $color) = (@_);
	foreach (values %$edges){
		drawEdge($_, $im, $thickness, $color);
	}
}

###################################################################
# Draw the given location on the given GD image, with a dot
# and a name label.
# Args:
#	- a reference to the location to print
#	- the GD image to print to
#	- the GD color object to use for the location's name
#	- the GD color object to use for the dot on the location's
#	  dot on the map.
# Returns:
#	- nothing
###################################################################
sub drawLocation{
	my($location, $im, $textColor, $dotColor) = (@_);

	# print the name of the location, at a slight offset
	$im->string(gdMediumBoldFont, $location->{'x'} + 5,
		$location->{'y'} - 6, $location->{'Name'}, $textColor);
	
	# ...and a dot!
	$im->filledRectangle(
		$location->{'x'} - 2, $location->{'y'} - 2,
		$location->{'x'} + 2, $location->{'y'} + 2, $dotColor);
}

sub drawAllLocations{
	my($locations, $im, $textColor, $dotColor) = (@_);
	foreach (values %$locations){
		drawLocation($_, $im, $textColor, $dotColor);
	}
}
1;
