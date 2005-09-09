#!/usr/bin/perl
# -----------------------------------------------------------------
# ll2pix.pm -- Convert lat/lons to pixel offsets.
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Fri Aug 12 13:51:01 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

sub ll2pix($$){
	my($x, $y) = @_;

	# first location
	my $lat_x1 = -117.234770;
	my $lat_y1 = 32.873778;

	# from the original map
	#my $pix_x1 = 2822;
	#my $pix_y1 = 5278;
	my $pix_x1 = 5287;
	my $pix_y1 = 5184;

	# second location
	my $lat_x2 = -117.243338;
	my $lat_y2 = 32.886275;

	# from the original map
	#my $pix_x2 = 824;
	#my $pix_y2 = 1842;
	my $pix_x2 = 3315;
	my $pix_y2 = 1725;

	my $scale_x = ($pix_x2 - $pix_x1)/($lat_x2 - $lat_x1);
	my $scale_y = ($pix_y2 - $pix_y1)/($lat_y2 - $lat_y1);

	my $pix_x = (($x - ($lat_x1)) * $scale_x) + $pix_x1;
	my $pix_y = (($y - ($lat_y1)) * $scale_y) + $pix_y1;

	return ($pix_x, $pix_y);
}

1;
