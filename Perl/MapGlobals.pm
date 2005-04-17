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
# Sat Apr 16 00:44:44 PDT 2005
# -----------------------------------------------------------------

package MapGlobals;

require Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(reaper getGd2Filename);
@EXPORT = qw(
	INFINITY TRUE FALSE MAX_SCALE MIN_SCALE
	$DYNAMIC_IMG_DIR $STATIC_IMG_DIR
);

# a few constants to make things more readable
use constant{
	INFINITY	=> ~0,
	TRUE		=> 1,
	FALSE		=> 0,
	MAX_SCALE	=> 1,
	MIN_SCALE	=> 0.125,
};

# some names for the various base images.
# TODO: clean this up! (remember old scripts that rely on the old names)
our $BASE_IMAGE		= 'UCSDmap.png';
our $_BASE_GD2_IMAGE	= 'UCSDmap';
our $BASE_GD2_IMAGE	= $_BASE_GD2_IMAGE . 'gd2';
our $OUT_IMAGE		= 'Output.png';

# size of the base image, in pixels
our $IMAGE_X = 7200;
our $IMAGE_Y = 6600;

# where to center when there isn't any selection.
# (2184, 3264) is Geisel Library
our $DEFAULT_XOFF = 2184;
our $DEFAULT_YOFF = 3264;

# locations of the binary files that contain the graph of paths
our $POINT_FILE		= 'binPointData.dat';
our $LOCATION_FILE	= 'binLocationData.dat';
our $EDGE_FILE		= 'binEdgeData.dat';

# where static images (such as the button graphics) are stored
our $STATIC_IMG_DIR	= '../../ucsdmap';
# where dynamically-generated images (map views) are stored
our $DYNAMIC_IMG_DIR	= $STATIC_IMG_DIR . '/dynamic';
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

# remove old files from $DYNAMIC_IMG_DIR.
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
