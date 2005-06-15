#!/usr/bin/perl
# -----------------------------------------------------------------
# MapGraphics.pl -- Functions for manipulating the map image.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Sun Jun 12 13:22:01 PDT 2005
# -----------------------------------------------------------------

package MapGraphics;

use strict;
use warnings;

use GD;
use MapGlobals qw(min max);

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
#	- a hash representing the bounding rectangle of this edge:
#	  {
#		xmin => upper-left corner's X coord
#		ymin => upper-left corner's Y coord
#		xmax => lower-right corner's X coord
#		ymax => lower-right corner's Y coord
#	  }
###################################################################
sub drawEdge{
	my($edge, $im, $thickness, $color, $xoff, $yoff, $w, $h, $scale, $force) = (@_);

	# draw all the Edge lines
	my $curpt;
	my $prevpt;

	my($xmin, $xmax, $ymin, $ymax);


	$im->setThickness($thickness);

	# cycle through each point in this Edge
	foreach $curpt ( @{$edge->{'Path'}} ){

		# keep the min/max values up to date
		$xmax = $curpt->{'x'} if( !defined($xmax) || $curpt->{'x'} > $xmax );
		$xmin = $curpt->{'x'} if( !defined($xmin) || $curpt->{'x'} < $xmin );
		$ymax = $curpt->{'y'} if( !defined($ymax) || $curpt->{'y'} > $ymax );
		$ymin = $curpt->{'y'} if( !defined($ymin) || $curpt->{'y'} < $ymin );

		if( defined($prevpt) ){
			# take the scale into account for each set of points
			$im->line(
				$prevpt->{'x'}*$scale - $xoff,
				$prevpt->{'y'}*$scale - $yoff,
				$curpt->{'x'}*$scale - $xoff,
				$curpt->{'y'}*$scale - $yoff,
				$color
			);
		}
		$prevpt = $curpt;
	}

	return ( xmin => $xmin, ymin => $ymin, xmax => $xmax, ymax => $ymax );
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
	my($location, $im, $textColor, $dotColor, $bgColor, $xoff, $yoff, $w, $h, $scale) = (@_);


	# these are the actual pixel locations (on the output image) where we 
	# start drawing the location name
	my $xDraw = $location->{'x'}*$scale - $xoff + 5;
	my $yDraw = $location->{'y'}*$scale - $yoff - 6;

	# where does this location's name end?
	my $textWidth = length($location->{'Name'}) * $MapGlobals::FONT_WIDTH;
	my $nameEnd = $xDraw + $textWidth;

	# don't bother going any further if the location won't appear on the screen
	if( $nameEnd < 0 || $xDraw - 5 > $w || $yDraw < 0 || $yDraw > $h ){
		return;
	}

	# check if it ends past the edge of the screen
	# AND the location is actually on the screen
	if($nameEnd > $w){
		# adjust the x-coord where we start drawing the location name
		$xDraw = ($location->{'x'}*$scale - $xoff) - 5 - $textWidth;
		$nameEnd = $xDraw + $textWidth;
	}

	if( defined($bgColor) ){
		$im->filledRectangle($xDraw - 3, $yDraw,
			$nameEnd, $yDraw + $MapGlobals::FONT_HEIGHT, $bgColor);
	}

	# print the name of the location, at a slight offset
	$im->string(
		# this font is 7x13; see FONT_WIDTH and FONT_HEIGHT in
		# MapGlobals.pm
		gdMediumBoldFont,
		$xDraw, $yDraw,
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
}

# XXX: proper desc. and function header
sub drawAllLocations{
	my($locations, $im, $textColor, $dotColor, $bgColor, $xoff, $yoff, $w, $h, $scale) = (@_);

	foreach (keys %$locations){
		if(!/:/){
			drawLocation($locations->{$_}, $im, $textColor, $dotColor, $bgColor,
				$xoff, $yoff, $w, $h, $scale);
		}
	}
}

sub drawLines{
	my($points, $im, $thickness, $color, $xoff, $yoff, $w, $h, $scale) = (@_);

	# draw all the Edge lines
	my $curpt;
	my $prevpt;

	$im->setThickness($thickness);

	# cycle through each point in this Edge
	foreach $curpt ( @$points ){
		if( defined($prevpt) ){
			# take the scale into account for each set of points
			$im->line(
				$prevpt->{'x'}*$scale - $xoff,
				$prevpt->{'y'}*$scale - $yoff,
				$curpt->{'x'}*$scale - $xoff,
				$curpt->{'y'}*$scale - $yoff,
				$color
			);
		}
		$prevpt = $curpt;
	}
}

1;
