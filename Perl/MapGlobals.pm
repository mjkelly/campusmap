# -----------------------------------------------------------------
# $Id$
# MapGlobals.pm -- $desc$
# Copyright 2005 David Lindquist and Michael Kelly
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
@EXPORT = qw(INFINITY TRUE FALSE);

use constant{
	INFINITY	=> ~0,
	TRUE		=> 1,
	FALSE		=> 0,
};

our $BASE_IMAGE		= 'UCSDmap.png';
our $OUT_IMAGE		= 'Output.png';

our $POINT_FILE		= 'binPointData.dat';
our $LOCATION_FILE	= 'binLocationData.dat';
our $EDGE_FILE		= 'binEdgeData.dat';

