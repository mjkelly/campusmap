#!/usr/bin/perl
# -----------------------------------------------------------------
# MapGraphics.pl -- Functions for manipulating the map image.
#
# Copyright 2005 Michael Kelly and David Lindquist
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
#	- X offset of the image we're drawing on (relative to original image)
#	- Y offset of the image we're drawing on (relative to original image)
#	- the width of the image we're drawing on (relative to original image)
#	- the height of the image we're drawing on (relative to original image)
#	- the scale multiplier (output image scale over original image scale)
# Returns:
#	- nothing
###################################################################
sub drawEdge{
	my($edge, $im, $thickness, $color, $xoff, $yoff, $w, $h, $scale, $force) = (@_);

	# draw all the Edge lines
	my $curpt;
	my $prevpt;

	# if the current point is in-bounds
	#my $curInBounds = 0;
	# if the previous point was in-bounds
	#my $prevInBounds = 0;

	$im->setThickness($thickness);

	# cycle through each point in this Edge
	foreach $curpt ( @{$edge->{'Path'}} ){
		if( defined($prevpt) ){
			# check if the current point is in-bounds
			#$curInBounds =
			#	($curpt->{'x'} - $xoff >= 0 && $curpt->{'x'} - $xoff <= $w
			#	&& $curpt->{'y'} - $yoff >= 0 && $curpt->{'y'} - $yoff <= $h);

			# if either the current or the previous point was in bounds,
			# we print (this is to allow for lines that go off the screen)
			#if($curInBounds || $prevInBounds || $force){
				# take the scale into account for each set of points
				$im->line(
					$prevpt->{'x'}*$scale - $xoff,
					$prevpt->{'y'}*$scale - $yoff,
					$curpt->{'x'}*$scale - $xoff,
					$curpt->{'y'}*$scale - $yoff,
					$color
				);
			#}

		}
		#$prevInBounds = $curInBounds;
		$prevpt = $curpt;
	}

}

# XXX: proper desc. and function header
sub drawAllEdges{
	my($edges, $im, $thickness, $color, $xoff, $yoff, $w, $h, $scale) = (@_);
	foreach (values %$edges){
		drawEdge($_, $im, $thickness, $color, $xoff, $yoff, $w, $h, $scale);
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
#	- X offset of the image we're drawing on (relative to original image)
#	- Y offset of the image we're drawing on (relative to original image)
#	- the width of the image we're drawing on (relative to original image)
#	- the height of the image we're drawing on (relative to original image)
#	- the scale multiplier (output image scale over original image scale)
# Returns:
#	- nothing
###################################################################
sub drawLocation{
	my($location, $im, $textColor, $dotColor, $xoff, $yoff, $w, $h, $scale) = (@_);

	# make sure we're drawing on the viewable window
	#if( $location->{'x'} - $xoff >= 0 && $location->{'x'} - $xoff <= $w
	# && $location->{'y'} - $yoff >= 0 && $location->{'y'} - $yoff <= $h ){
	 	# all the dot locations are adjusted for the scale before being passed
		# to the lower-level routines

		# print the name of the location, at a slight offset
		$im->string(
			gdMediumBoldFont,
			$location->{'x'}*$scale - $xoff + 5,
			$location->{'y'}*$scale - $yoff - 6,
			$location->{'Name'},
			$textColor
		);
		
		# ...and a dot!
		$im->filledRectangle(
			$location->{'x'}*$scale - $xoff - 2,
			$location->{'y'}*$scale - $yoff - 2,
			$location->{'x'}*$scale - $xoff + 2,
			$location->{'y'}*$scale - $yoff + 2,
			$dotColor
		);

	#}
}

# XXX: proper desc. and function header
sub drawAllLocations{
	my($locations, $im, $textColor, $dotColor, $xoff, $yoff, $w, $h, $scale) = (@_);

	foreach (keys %$locations){
		if(!/:/){
			drawLocation($locations->{$_}, $im, $textColor, $dotColor,
				$xoff, $yoff, $w, $h, $scale);
		}
	}
}

1;
