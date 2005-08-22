#!/usr/bin/perl
# -----------------------------------------------------------------
# MapGraphics.pl -- Functions for manipulating the map image.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Wed Jun 22 13:22:14 PDT 2005
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

###################################################################
# Um... draw... all... the... edges.
# XXX: DEPRECATED by the new way of loading edges on-demand.
#
# Args:
#	- a hashref containing all the edges on the map
#	- the GD image to print to
#	- the GD color object to use for the location's name
#	- the GD color object to use for the the location's dot
#	- X offset of the image we're drawing on (relative to original image)
#	- Y offset of the image we're drawing on (relative to original image)
#	- the width of the image we're drawing on (relative to original image)
#	- the height of the image we're drawing on (relative to original image)
#	- the scale multiplier (output image scale over original image scale)
###################################################################
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
#	- the GD color object to use for the the location's dot
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

	# check if it ends past the right edge of the screen
	# AND the new xDraw location isn't past the LEFT edge of the screen
	# (yes, it's ugly)
	if($nameEnd > $w && ($location->{'x'}*$scale - $xoff) - 5 - $textWidth > 0){
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

###################################################################
# Draw all locations on the screen. Obviously, only the ones
# actually appearing in the current viewport will appear.
# Args:
#	- a reference to the locations hashref
#	- the GD image to print to
#	- the GD color object to use for the location's name
#	- the GD color object to use for the location's dot
#	- X offset of the image we're drawing on (relative to original image)
#	- Y offset of the image we're drawing on (relative to original image)
#	- the width of the image we're drawing on (relative to original image)
#	- the height of the image we're drawing on (relative to original image)
#	- the scale multiplier (output image scale over original image scale)
# Returns:
#	- nothing
###################################################################
sub drawAllLocations{
	my($locations, $im, $textColor, $dotColor, $bgColor, $xoff, $yoff, $w, $h, $scale) = (@_);

	foreach (keys %$locations){
		if(!/:/){
			drawLocation($locations->{$_}, $im, $textColor, $dotColor, $bgColor,
				$xoff, $yoff, $w, $h, $scale);
		}
	}
}

###################################################################
# Given an arraref of arrayrefs (each of which represents an edge) of points
# (hashrefs with 'x' and 'y' keys), draw these to the screen.
# Args:
#	- 'points' arrayref: this consists of a series of smaller arrayrefs,
#	  each of which contains hashrefs with 'x' and 'y' keys, representing
#	  individual points. Lines are drawn between each of the points in each
#	  smaller arrayref. (The ends of the arrayrefs themselves are not
#	  connected.)
#	- the GD image to print to
#	- the thickness of the line to draw, in pixels
#	- the GD color object to use for the line
#	- X offset of the image we're drawing on (relative to original image)
#	- Y offset of the image we're drawing on (relative to original image)
#	- the width of the image we're drawing on (relative to original image)
#	- the height of the image we're drawing on (relative to original image)
#	- the scale multiplier (output image scale over original image scale)
###################################################################
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

###################################################################
# A version of drawLines that handles offsets differently: xoff and yoff are
# adjusted for scale.
# Args:
#	- same as drawLines
###################################################################
sub drawLinesRaw{
	my($points, $im, $thickness, $color, $xoff, $yoff, $w, $h, $scale) = (@_);

	# draw all the Edge lines
	my $curpt;
	my $prevpt;

	$im->setThickness($thickness);

	#warn "drawLinesRaw: offsets: ($xoff, $yoff), size: ($w, $h)\n";

	# cycle through each point in this Edge
	foreach $curpt ( @$points ){
		if( defined($prevpt) ){
			# take the scale into account for each set of points
			$im->line(
				$scale*($prevpt->{'x'} - $xoff),
				$scale*($prevpt->{'y'} - $yoff),
				$scale*($curpt->{'x'} - $xoff),
				$scale*($curpt->{'y'} - $yoff),
				$color
			);
		}
		#warn "drawing: (" . ($curpt->{'x'} - $xoff) . ", " . ($curpt->{'y'} - $yoff) . ")\n";
		$prevpt = $curpt;
	}
}

1;
