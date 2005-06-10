# -----------------------------------------------------------------
# MapGlobals.pm -- Global variables and a few general-purpose 
# subroutines.
#
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Wed May 11 21:20:14 PDT 2005
# -----------------------------------------------------------------

package MapGlobals;

# a few constants to make things more readable
use constant{
	INFINITY	=> ~0,
	TRUE		=> 1,
	FALSE		=> 0,
};

require Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(reaper getGd2Filename @SCALES);
@EXPORT = qw(
	INFINITY TRUE FALSE
	$DYNAMIC_IMG_DIR $STATIC_IMG_DIR
);

# a list of scaling attributes
# we use indexes in the application, then map them to the acual multiplier
# values internally. Remember, these MUST be multiplier values, because the low-level
# drawing routines use them to scale path/location draws.
our @SCALES = (1, 0.5, 0.25, 0.125);

# some names for the various base images.
# TODO: clean this up! (remember old scripts that rely on the old names)
our $BASE_IMAGE		= 'UCSDmap.png';
our $_BASE_GD2_IMAGE	= 'UCSDmap';
our $BASE_GD2_IMAGE	= $_BASE_GD2_IMAGE . 'gd2';
our $OUT_IMAGE		= 'Output.png';

our $TEMPLATE		= 'template.html';

# size of the base image, in pixels
our $IMAGE_X = 7200;
our $IMAGE_Y = 6600;

# size of the thumbnail image
our $THUMB_X = 192;
our $THUMB_Y = 176;
our $THUMB_FILE = 'thumbnail.png';

# store the ratio between the thumbnail and the main base image
# (these two REALLY should be the same...)
our $RATIO_X = $THUMB_X / $IMAGE_X;
our $RATIO_Y = $THUMB_Y / $IMAGE_Y;

# where to center when there isn't any selection.
# (2184, 3264) is Geisel Library
our $DEFAULT_XOFF = 2184;
our $DEFAULT_YOFF = 3264;

# how many pixels (on the largest base map) equal one meter
# From tests, we've determined that 288696.00 px is approximately 0.72 mi.
# that gives us ~400967 px per mile.
our $PIXELS_PER_UNIT = 400967;
# an abbreviation of the unit we're using (m for meters, mi for miles, etc)
our $UNITS = "mi";

# locations of the binary files that contain the graph of paths
our $POINT_FILE		= 'binPointData.dat';
our $LOCATION_FILE	= 'binLocationData.dat';
our $EDGE_FILE		= 'binEdgeData.dat';

# where static images (such as the button graphics) are stored
our $STATIC_IMG_DIR	= '../../ucsdmap';
##our $STATIC_IMG_DIR	= '../images';
# where dynamically-generated images (map views) are stored
our $DYNAMIC_IMG_DIR	= '../../ucsdmap/dynamic';
##our $DYNAMIC_IMG_DIR	= '../images/dynamic';
# the suffix of all dynamically-generated images; used for matching for
# deletion
our $DYNAMIC_IMG_SUFFIX	= '.png';
# maximum age, in seconds, of dynamically generated images
our $DYNAMIC_MAX_AGE	= 5*60;

# return a the filename of GD2 image file corresponding to the given scale
sub getGd2Filename{
	my ($scale) = (@_);
	return $_BASE_GD2_IMAGE . '-' . $scale . '.gd2';
}

###################################################################
# Remove old dynamic files from $DYNAMIC_IMG_DIR.
# Args:
#	- maximum allowed age of files, in seconds
#	- the suffix of files to be killed
###################################################################
sub reaper{
	my($max_age, $kill_suffix) = (@_);
	my $now = time();

	opendir(DIR, $DYNAMIC_IMG_DIR);

	while( my $file = readdir(DIR) ){
		# skip dotfiles
		next if(substr($file, 0, 1) eq '.');

		# skip files that don't match the suffix
		next if( substr($file, -(length($kill_suffix))) ne $kill_suffix);

		# get the age of the file in seconds
		my $age = $now -(stat("$DYNAMIC_IMG_DIR/$file"))[9];

		# kill it if it's too old
		if($age > $max_age){
			#warn "reaper @ $now: $file: chop, chop, chop!\n";
			unlink("$DYNAMIC_IMG_DIR/$file");
		}
	}

	closedir(DIR);
}
