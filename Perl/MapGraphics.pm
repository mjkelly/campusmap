# vim: tabstop=4 shiftwidth=4
# -----------------------------------------------------------------
# MapGraphics.pm -- Functions for manipulating the map image.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# $Id$
# -----------------------------------------------------------------

package MapGraphics;

use strict;
use warnings;

use GD;
use File::Temp;
use File::Basename;
use MapGlobals qw(min max @SCALES plog);

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

###################################################################
# Create thumbnail image representing the given state. Notice that this isn't a
# "draw*" function, because it doesn't draw to an existing GD image -- it
# creates its own.
# Args:
#	- ID of source location, or undef if there is none
#	- ID of destination location, or undef if there is none
#	- the current scale, as an index
#	- the width of the viewport
#	- the height of the viewport
#	- the effective X offset (this is norm_xoff in map.cgi)
#	- the effective Y offset (this is norm_yoff in map.cgi)
#	- locations hashref
# Returns:
#	- the name of the temporary file
###################################################################
sub makeThumbnail{
	my($from, $to, $scale, $w, $h, $norm_xoff, $norm_yoff, $locations) = @_;

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
		($norm_xoff - ($w/$SCALES[$scale])/2)*$MapGlobals::RATIO_X,
		($norm_yoff - ($h/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y,
		($norm_xoff + ($w/$SCALES[$scale])/2)*$MapGlobals::RATIO_X - 1,
		($norm_yoff + ($h/$SCALES[$scale])/2)*$MapGlobals::RATIO_Y - 1,
		$thumb_rect_color
	);

	# dots for the start and end locations
	if(defined($from)){
		$thumb->filledRectangle(
			$locations->{'ByID'}{$from}{'x'}*$MapGlobals::RATIO_X - 1,
			$locations->{'ByID'}{$from}{'y'}*$MapGlobals::RATIO_Y - 1,
			$locations->{'ByID'}{$from}{'x'}*$MapGlobals::RATIO_X + 1,
			$locations->{'ByID'}{$from}{'y'}*$MapGlobals::RATIO_Y + 1,
			$thumb_src_color,
		);
	}

	if(defined($to)){
		$thumb->filledRectangle(
			$locations->{'ByID'}{$to}{'x'}*$MapGlobals::RATIO_X - 1,
			$locations->{'ByID'}{$to}{'y'}*$MapGlobals::RATIO_Y - 1,
			$locations->{'ByID'}{$to}{'x'}*$MapGlobals::RATIO_X + 1,
			$locations->{'ByID'}{$to}{'y'}*$MapGlobals::RATIO_Y + 1,
			$thumb_dst_color,
		);
	}

	# now make a temporary file to put this image in
	# XXX: eventually just make this a separate CGI script?
	# I don't know -- which has higher cost: creating/deleting a file, or starting
	# another perl process?
	my $tmpthumb = new File::Temp(
		TEMPLATE => 'thumb-XXXXXX',
		DIR => $MapGlobals::DYNAMIC_IMG_PATH,
		SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
		UNLINK => 0,
	);
	chmod(0644, $tmpthumb->filename);

	# write out the finished thumbnail to the file
	binmode($tmpthumb);
	print $tmpthumb $thumb->png();
	close($tmpthumb);

	return $MapGlobals::DYNAMIC_IMG_DIR . '/' . basename($tmpthumb->filename);
}

###################################################################
# Make a small temporary image zoomed on a specific location, and return the
# filename of the image so created.
#
# Args:
#	- locations hashref
#	- pathpoints arrayref (as from ShortestPath::pathPoints() or
#	  LoadData::loadCache()), or undef if there is no path to print
#	- the name of the current map (key into %MapGlobals::MAPS)
#	- ID of the location to zoom in on
#	- arrayref specifying the color to use to print the location.
#	  (i.e., [255, 0, 0] for red, [200, 200, 200] for gray, etc.)
# Returns:
#	- filename of the temporary image created
###################################################################
sub makeZoomImage{
	my($locations, $pathPoints, $mapname, $id, $color) = @_;
	my $x = $locations->{'ByID'}{$id}{'x'}*$SCALES[$MapGlobals::LITTLE_WINDOW_SCALE] - $MapGlobals::LITTLE_WINDOW_X/2;
	my $y = $locations->{'ByID'}{$id}{'y'}*$SCALES[$MapGlobals::LITTLE_WINDOW_SCALE] - $MapGlobals::LITTLE_WINDOW_Y/2;

	# get the image of the appropriate scale from disk, and grab only
	# what we need by size and offset
	my $im = GD::Image->newFromGd2Part(MapGlobals::getGd2Filename($mapname, $MapGlobals::LITTLE_WINDOW_SCALE),
		$x, $y, $MapGlobals::LITTLE_WINDOW_X, $MapGlobals::LITTLE_WINDOW_Y);

	my $loc_color = $im->colorAllocate(@$color);
	my $path_color = $im->colorAllocate(@MapGlobals::PATH_COLOR);
	my $bg_color = $im->colorAllocate(@MapGlobals::LOC_BG_COLOR);

	# draw the path
	if(defined($pathPoints)){
		foreach my $line (@$pathPoints){
			MapGraphics::drawLines($line, $im, $MapGlobals::PATH_THICKNESS, $path_color, $x, $y,
				$MapGlobals::LITTLE_WINDOW_X, $MapGlobals::LITTLE_WINDOW_Y, $SCALES[$MapGlobals::LITTLE_WINDOW_SCALE]);
		}
	}

	# draw the location
	MapGraphics::drawLocation($locations->{'ByID'}{$id}, $im, $loc_color, $loc_color, $bg_color,
		$x, $y, $MapGlobals::LITTLE_WINDOW_X, $MapGlobals::LITTLE_WINDOW_Y, $SCALES[$MapGlobals::LITTLE_WINDOW_SCALE]);


	# write out the images
	my $file = new File::Temp(
		TEMPLATE => 'zoom-XXXXXX',
		DIR => $MapGlobals::DYNAMIC_IMG_PATH,
		SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
		UNLINK => 0,
	);
	chmod(0644, $file->filename);
	binmode($file);
	print $file $im->png();
	close($file);
	
	return $MapGlobals::DYNAMIC_IMG_DIR . '/' . basename($file->filename);
}

###################################################################
# Write the main map window. This is the guts of the 'plain' view.
# Creates a temporary file to store the image, and returns that filename.
#
# Args (yeah, I know, there are a lot...)
#	- locations hashref
#	- path points arrayref (from LoadData::loadCache()), or undef if there
#	  is no path to print
#	- the name of the current map (%MapGlobals::MAP key)
#	- ID of source location, or undef if none
#	- ID of destination location, or undef if none
#	- width of the viewport
#	- height of the viewport
#	- raw X offset (upper-left corner, in scaled pixels)
#	- raw Y offset (upper-left corner, in scaled pixels)
#	- current scale (index)
# Returns:
#	- the filename of the created image
###################################################################
sub makeMapImage{
	my($locations, $pathPoints, $mapname, $from, $to, $width, $height, $rawxoff, $rawyoff, $scale) = @_;
	# get the image of the appropriate scale from disk, and grab only
	# what we need by size and offset
	my $im = GD::Image->newFromGd2Part(MapGlobals::getGd2Filename($mapname, $scale),
		$rawxoff, $rawyoff, $width, $height);

	my $src_color = $im->colorAllocate(@MapGlobals::SRC_COLOR);
	my $dst_color = $im->colorAllocate(@MapGlobals::DST_COLOR);
	my $path_color = $im->colorAllocate(@MapGlobals::PATH_COLOR);
	my $bg_color = $im->colorAllocate(@MapGlobals::LOC_BG_COLOR);

	# uncomment to draw ALL locations
	#MapGraphics::drawAllLocations($locations, $im, $src_color, $src_color, $bg_color, $rawxoff, $rawyoff,
	#				$width, $height, $SCALES[$scale]);

	# if we had a path to draw, now's the time to do it
	if(defined($pathPoints)){
		foreach my $line (@$pathPoints){
			MapGraphics::drawLines($line, $im, $MapGlobals::PATH_THICKNESS, $path_color, $rawxoff, $rawyoff,
				$width, $height, $SCALES[$scale]);
		}
	}

	# draw the source and destination locations, if they've been found
	if(defined($from)){
		MapGraphics::drawLocation($locations->{'ByID'}{$from}, $im, $src_color, $src_color, $bg_color,
			$rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);
	}
	if(defined($to)){
		MapGraphics::drawLocation($locations->{'ByID'}{$to}, $im, $dst_color, $dst_color, $bg_color,
			$rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);
	}


	# generate a temporary file on disk to store the map image
	my $tmpfile = new File::Temp(
		TEMPLATE => 'map-XXXXXX',
		DIR => $MapGlobals::DYNAMIC_IMG_PATH,
		SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
		UNLINK => 0,
	);
	chmod(0644, $tmpfile->filename);

	# print the data out to a temporary file
	binmode($tmpfile);
	print $tmpfile $im->png();
	close($tmpfile);

	return $MapGlobals::DYNAMIC_IMG_DIR . '/' . basename($tmpfile->filename);
}


###################################################################
# Create all the transparent path images for a given path.
#
# Args:
#	- source location ID
#	- destination location ID
#	- hashref representing the path's bounding rectangle (from loadCache()
#	  or pathPoints())
#	- arrayref of path points (also from loadCache() or pathPoints())
# Returns:
#	- a bounding rectangle for the _path image_, at the base (scale = 1)
#	  level. This is NOT necessarily the same as the bounding rectangle
#	  for the path itself!
###################################################################
sub makePathImages{
	my($from, $to, $rect, $pathPoints) = @_;

	plog("Making path image for $from --> $to.\n");

	# a little padding 
	my $padding = 32;
	# if we have a path between two locations, write the path images
	
	my $pathImgRect = {
		xmin => $rect->{'xmin'} - $padding,
		ymin => $rect->{'ymin'} - $padding,
		xmax => $rect->{'xmax'} + $padding,
		ymax => $rect->{'ymax'} + $padding,
	};
	my $pathWidth = $pathImgRect->{'xmax'} - $pathImgRect->{'xmin'};
	my $pathHeight = $pathImgRect->{'ymax'} - $pathImgRect->{'ymin'};

	my $curScale;
	for my $i (0 .. $#SCALES){
		$curScale = $SCALES[$i];

		if(! -e MapGlobals::getPathFilename($from, $to, $i) ){
			# since we're creating new ones, delete old path files
			##MapGlobals::reaper($MapGlobals::PATH_IMG_PATH, $MapGlobals::PATH_MAX_AGE, $MapGlobals::DYNAMIC_IMG_SUFFIX);

			#warn "generating scale $i: $curScale\n";
			
			my $im = GD::Image->new($pathWidth*$curScale, $pathHeight*$curScale);
			my $bg_color = $im->colorAllocate(0, 0, 0);
			$im->transparent($bg_color);
			my $path_color = $im->colorAllocate(@MapGlobals::PATH_COLOR);

			foreach my $line (@$pathPoints){
				MapGraphics::drawLinesRaw($line, $im,
					$MapGlobals::PATH_THICKNESS, $path_color,
					$rect->{'xmin'} - $padding,
					$rect->{'ymin'} - $padding,
					$pathWidth, $pathHeight, $curScale);
			}

			my $fname = MapGlobals::getPathFilename($from, $to, $i);
			open(OUTFILE, '>', $fname) or die "Cannot open output path file '$fname': $!\n";
			binmode(OUTFILE);
			print OUTFILE $im->png();
			close(OUTFILE);
			chmod(0644, $fname);
		}
	}

	return $pathImgRect;
}

1;
