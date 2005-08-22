# -----------------------------------------------------------------
# MapGlobals.pm -- Global variables and a few general-purpose 
# subroutines.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Sun Jun 12 13:22:08 PDT 2005
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
@EXPORT_OK = qw(TRUE FALSE INFINITY between max min asInt round @SIZES @SCALES);
@EXPORT = qw();

# a list of scaling attributes
# we use indexes in the application, then map them to the acual multiplier
# values internally. Remember, these MUST be multiplier values, because the low-level
# drawing routines use them to scale path/location draws.
our @SCALES = (1, 0.5, 0.25, 0.125);

# map display sizes.
our @SIZES = (
	[400, 300],
	[500, 375],
	[640, 480],
);


# some names for the various base images.
# TODO: clean this up! (remember old scripts that rely on the old names)
our $BASE_IMAGE		= 'UCSDmap.png';
our $_BASE_GD2_IMAGE	= 'UCSDmap';
our $BASE_GD2_IMAGE	= $_BASE_GD2_IMAGE . 'gd2';

our %TEMPLATES = (
	plain => 'template3.html',
	print => 'print_tmpl.html',
	js => 'js_tmpl.html',
);

# size of the base image, in pixels
our $IMAGE_X = 7200;
our $IMAGE_Y = 6600;

# size of the thumbnail image
#our $THUMB_X = 192;
#our $THUMB_Y = 176;
our $THUMB_X = 144;
our $THUMB_Y = 132;
our $THUMB_FILE = 'thumbnail.gd2';

# _URL_ to the 
our $CSS_FILE = '../../ucsdmap/css/main.css';
##our $CSS_FILE = '/css/main.css';

# how wide the font that we use for drawling location names is, in pixels
our $FONT_WIDTH = 7;
our $FONT_HEIGHT = 13;

# maximum number of characters of a location's name to display in the
# drop-down menu
our $MAX_NAME_LEN = 30;

# how thick are the paths we draw between locations? (in pixels)
our $PATH_THICKNESS = 4;

# colors!
# these are triplets of values between 0 and 255 (inclusive)
# color associated with 'from' location
our @SRC_COLOR = (0, 255, 0);
# color associated with 'to' location
our @DST_COLOR = (255, 0, 0);
# color paths are drawn in
#our @PATH_COLOR = (0, 0, 255);
our @PATH_COLOR = (51, 51, 204);
# color the viewport outline rectangle (in the thumbnail) is drawn in
our @RECT_COLOR = (0, 0, 255);
# color location background text (@SRC_COLOR and @DST_COLOR) is drawn in
our @LOC_BG_COLOR = (100, 100, 100);

# store the ratio between the thumbnail and the main base image
# (these two REALLY should be the same...)
our $RATIO_X = $THUMB_X / $IMAGE_X;
our $RATIO_Y = $THUMB_Y / $IMAGE_Y;

# where to center when there isn't any selection.
# (2184, 3264) is Geisel Library
our $DEFAULT_XOFF  = 2184;
our $DEFAULT_YOFF  = 3264;
our $DEFAULT_SCALE = 3; #index into @SCALES
# this is the scale we use when we're zoomed to a single
# location (a real one). also an index into @SCALES.
our $SINGLE_LOC_SCALE = 1;

# how many pixels (on the largest base map) equal one meter
# From tests, we've determined that 288696.00 px is approximately 0.72 mi.
# that gives us ~400967 px per mile.
#our $PIXELS_PER_UNIT = 400967; # <--- this was when we multiplied all distances by 100
our $PIXELS_PER_UNIT = 4010;
# an abbreviation of the unit we're using (m for meters, mi for miles, etc)
our $UNITS = "mi";

# locations of the binary files that contain the graph of paths
our $POINT_FILE		= 'binPointData.dat';
our $LOCATION_FILE	= 'binLocationData.dat';
our $EDGE_FILE		= 'binEdgeData.dat';

# where cache files (which store the coordinates of shortest paths) go
our $CACHE_DIR		= 'cache';
# how long cache files hang around for, in seconds
our $CACHE_EXPIRY	= 10*60;

# where static content is stored
our $HTML_BASE		= '../../ucsdmap';
##our $HTML_BASE		= '..';

our $CSS_DIR		= "$HTML_BASE/css";
our $STATIC_IMG_DIR	= "$HTML_BASE/images";
our $DYNAMIC_IMG_DIR	= "$HTML_BASE/dynamic";
our $PATH_IMG_DIR	= $DYNAMIC_IMG_DIR . '/paths';
our $GRID_IMG_DIR	= $STATIC_IMG_DIR . '/grid';

# the suffix of all dynamically-generated images; used for matching for
# deletion
our $DYNAMIC_IMG_SUFFIX	= '.png';
# maximum age, in seconds, of dynamically generated images
our $DYNAMIC_MAX_AGE	= 10*60;

# how long path images stay up
our $PATH_MAX_AGE	= 10*60;

# we also have some basic utility functions in here, that any part
# of the script may want to use

###################################################################
# Given a scale, return the path to the GD2 base image at that scale.
# Does NOT check if the file exists.
# Args:
#	- the scale (as a multiplier, not an array offset)
# Returns:
#	- the path to the GD2 base image at that scale. File might
#	  not exist.
###################################################################
sub getGd2Filename{
	my ($scale) = (@_);
	return $_BASE_GD2_IMAGE . '-' . $scale . '.gd2';
}

###################################################################
# Get given the IDs of two locations, return the filename to be used 
# for the cache of their shortest path. The order in which the location IDs
# appear in the argument list is not significant.
#
# Args:
# 	- a location ID
#	- another location ID
# Returns:
# 	- filename for a cache file of the two args.
###################################################################
sub getCacheName{
	my($from, $to) = (asInt(shift()), asInt(shift()));
	return $CACHE_DIR . '/' . min($from, $to) . '-' . max($from, $to) . '.cache';
}


###################################################################
# Get the filename for a path image between the given locations, at the given
# zoom level.
# Args:
# 	- source location ID
#	- destination location ID
#	- zoom level, as index (not multiplier)
# Returns:
# 	- filename of path file for given arguments
###################################################################
sub getPathFilename{
	my($src, $dst, $zoom) = @_;
	# untaint
	$src = asInt($src);
	$dst = asInt($dst);
	$zoom = asInt($zoom);
	return $PATH_IMG_DIR
		. '/im-' . min($src, $dst) . '-' . max($src, $dst) . '-' . $zoom . '.png';
}

###################################################################
# Remove old dynamic files from $DYNAMIC_IMG_DIR.
# Args:
#	- the directory to reap
#	- maximum allowed age of files, in seconds
#	- the suffix of files to be killed
# Returns:
#	- the number of heads on the floor
###################################################################
sub reaper{
	# Don't fear the reaper...
	my($dir, $max_age, $kill_suffix) = (@_);
	my $now = time();

	#warn "reaper: checking dir $dir for $kill_suffix files older than $max_age secs...\n";
	opendir(DIR, $dir);

	while( my $file = readdir(DIR) ){
		# skip dotfiles
		next if(substr($file, 0, 1) eq '.');

		# skip files that don't match the suffix
		next if( substr($file, -(length($kill_suffix))) ne $kill_suffix);

		# get the age of the file in seconds
		my $age = $now -(stat("$dir/$file"))[9];

		# kill it if it's too old
		if($age > $max_age){
			# make sure the filename is roughly of the correct format,
			# to avoid deleting random stuff that somehow got in here
			# (yes, we're doing this twice: this time it's to
			# appease taint mode)
			if($file =~ /^([-\w]+)$kill_suffix$/){
				$file = $1 . $kill_suffix;
				#warn "reaper @ $now: $file: chop, chop, chop!\n";
				unlink("$dir/$file");
			}
		}
	}
	# more cowbell!

	closedir(DIR);
}

###################################################################
# Given numbers A, B, and C, returns the value that is as close to C as
# possible while still being in [A, B].
# Args:
#	- minimum value (A)
#	- maximum value (B)
#	- target value (C)
# Returns:
#	- Value in [A, B] that's closest to the target value.
###################################################################
sub between{
	my($min, $max, $val) = (@_);
	if($val < $min){
		return $min;
	}
	if($val > $max){
		return $max;
	}
	return $val;
}

###################################################################
# Return the smaller of two numeric values.
# Args:
#	- X
#	- Y
# Returns:
#	- the smaller of X and Y
###################################################################
sub min{
	my($x, $y) = @_;
	if($x < $y){
		return $x;
	}
	else{
		return $y;
	}
}

###################################################################
# Return the greater of two numeric values.
# Args:
#	- X
#	- Y
# Returns:
#	- the greater of X and Y
###################################################################
sub max{
	my($x, $y) = @_;
	if($x > $y){
		return $x;
	}
	else{
		return $y;
	}
}

###################################################################
# This is really just int(), but it has a few important advantages for our
# purposes:
#	- it untaints the variable passed in (because that's the whole point of
#	  running numeric input through int()!)
#	- doesn't give warnings when it gets a string (strings == 0)
#
# Args:
#	- any scalar
# Returns:
#	- the argument as an integer (this is 0 for most strings)
###################################################################
sub asInt{
	my $str = shift;
	my ($r) = ($str =~ /^(\d+)/);
	if($r){
		return $r;
	}
	else{
		return 0;
	}
}

###################################################################
# Round the given number to the nearest whole number. Straight from the Perl
# FAQ, 4.13.
#
# Args:
#	- the number to round
# Returns:
#	- the given number, rounded to the nearest whole number
###################################################################
sub round{
	my $n = shift;
	return int($n + .5 * ($n <=> 0));
}
