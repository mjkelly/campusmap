# -----------------------------------------------------------------
# MapGlobals.pm -- $desc$
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Thu Mar 24 18:30:48 PST 2005
# -----------------------------------------------------------------

package MapGlobals;

require Exporter;
@ISA = qw(Exporter);
#@EXPORT_OK = qw();
@EXPORT = qw(INFINITY TRUE FALSE MAX_SCALE MIN_SCALE);

use constant{
	INFINITY	=> ~0,
	TRUE		=> 1,
	FALSE		=> 0,
	MAX_SCALE	=> 1,
	MIN_SCALE	=> 0.25,
};

our $BASE_IMAGE		= 'UCSDmap.png';
our $_BASE_GD2_IMAGE	= 'UCSDmap';
our $BASE_GD2_IMAGE	= $_BASE_GD2_IMAGE . 'gd2';
our $OUT_IMAGE		= 'Output.png';

our $IMAGE_X = 7200;
our $IMAGE_Y = 6600;

our $POINT_FILE		= 'binPointData.dat';
our $LOCATION_FILE	= 'binLocationData.dat';
our $EDGE_FILE		= 'binEdgeData.dat';

# return a the filename of GD2 image file corresponding to the given scale
sub getGd2Filename{
	my ($scale) = (@_);
	return $_BASE_GD2_IMAGE . '-' . $scale . '.gd2';
}
