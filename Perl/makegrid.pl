#!/usr/bin/perl
# -----------------------------------------------------------------
# makegrid.pl -- $desc$
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Fri Aug  5 12:40:22 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use GD;

my $debug = 1;

my $width  = 200;
my $height = 200;

my @sizes = (
	[7200, 6600],
	[3600, 3300],
	[1800, 1650],
	[900,  825 ],
);
my $zoom = int(shift(@ARGV)) || 0;

my $base_name  = 'map';

#my $base_image = "grid_bases/scale-$zoom.gd2";
my $base_image = "../color-map/scale-$zoom.gd2";
my $out_dir = 'grid';

my($x, $y);
my $filename;


if($debug){
	print "Reading from base image: $base_image\n";
}

$y = 0;
while($y*$height < $sizes[$zoom][1]){
	$x = 0;
	while($x*$width < $sizes[$zoom][0]){
		printf("Opening @ (%d, %d)\n", $x*$width, $y*$height);

		$filename = "$out_dir/$base_name-${zoom}[$y][$x].png";
		
		if($debug){
			print "Writing to: $filename\n";
		}
		else{
			my $im = GD::Image->newFromGd2Part($base_image, $x*$width, $y*$height, $width, $height);
			if(!defined($im)){
				die "Image object undefined! (Error opening base image?)\n";
			}

			open(OUT, '>', $filename) or die "Foo! $!\n";
			binmode(OUT);
			print OUT $im->png();
			close(OUT);
		}

		$x++;
	}
	$y++;
}

print "gridMaxX = $x\n";
print "gridMaxY = $y\n";

if($debug){
	print "Debug mode -- dry run only. Turn off debug mode to write images.\n";
}
