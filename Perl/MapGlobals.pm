# vim: tabstop=4 shiftwidth=4
# -----------------------------------------------------------------
# MapGlobals.pm -- Global variables and a few general-purpose 
# subroutines.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# $Id$
# -----------------------------------------------------------------

package MapGlobals;

# -----------------------------------------------------------------
# Basic constants.
# -----------------------------------------------------------------
use constant{
	INFINITY	=> ~0,
	TRUE		=> 1,
	FALSE		=> 0,
};

require Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(TRUE FALSE INFINITY between max min asInt round getWords @SIZES @SCALES plog);
@EXPORT = qw();

# -----------------------------------------------------------------
# Defaults.
# -----------------------------------------------------------------
# NOTE: Many of these values are indices into arrays or hashes declared later.
# Look at the corresponding array/hash and they'll make more sense.

# index into @SIZES when it's not specified
our $DEFAULT_SIZE = 1;

# if we get no input from the user, or it's invalid, which map do we choose?
# this is a key in %MAPS.
our $DEFAULT_MAP = 'visitor';

# key into %TEMPLATES
our $DEFAULT_TEMPLATE = 'js';

# where to center when there isn't any selection.
# (4608, 3172) is Geisel Library
our $DEFAULT_XOFF  = 4608;
our $DEFAULT_YOFF  = 3172;
our $DEFAULT_SCALE = 3; #index into @SCALES

# the default number of minutes it takes to walk one mile, if the user doesn't
# specify
our $DEFAULT_MPM = 20;

# -----------------------------------------------------------------
# Sizes.
# -----------------------------------------------------------------
# we use indexes in the application, then map them to the acual multiplier
# values internally. Remember, these MUST be multiplier values, because the low-level
# drawing routines use them to scale path/location draws.
#our @SCALES = (1, 0.5, 0.25, 0.125, 0.0625);
our @SCALES = (0.5, 0.25, 0.125, 0.0625);

# map viewport sizes, in pixels.
our @SIZES = (
	[400, 300],
	[500, 375],
	[640, 480],
);

# the size of the little print-view windows for the source and destination
# locations, in pixels
our $LITTLE_WINDOW_X = 150;
our $LITTLE_WINDOW_Y = 150;
# how far do we zoom in? (index into @SCALES)
our $LITTLE_WINDOW_SCALE = 0;

# size of the base image, in pixels
our $IMAGE_X = 9568;
our $IMAGE_Y = 8277;

# size of the thumbnail image, in pixels. This is not necessarily passed
# through to the templates, but is used for drawing the locations and viewport
# on the thumb image
our $THUMB_X = 150;
our $THUMB_Y = 130;

# store the ratio between the thumbnail and the main base image
# (these two should be the same or very very close...)
our $RATIO_X = $THUMB_X / $IMAGE_X;
our $RATIO_Y = $THUMB_Y / $IMAGE_Y;

# -----------------------------------------------------------------
# Filenames.
# -----------------------------------------------------------------
# the name of the main interface script, used for form actions, etc
our $SELF		= 'map.cgi';

# where we redirect stdout
our $LOG_FILE	= 'map_log.txt';
# log messages must be at least this level to be printed to the log file
our $LOG_LEVEL	= 1;

# prefix for the base images (relative to the script)
our $_BASE_NAME		= 'base-images/map';

# The series of scaled base images we may use. The key is what is passed in
# from the outside, and value is what is incorporated into the filename.
# (There's no particular reason why they would be different, but there they
# are.)
our %MAPS = (
	visitor => 'visitor',
);

# match query-string arguments to template filenames
our %TEMPLATES = (
	plain => 'template3.html',
	js => 'js_tmpl.html',
	print => 'print_tmpl.html',
);

# where static content is stored
our $HTML_BASE		= '../../ucsdmap';		# url
##our $HTML_BASE		= '..';			# url

# subdirectories of $HTML base for specific types of data
our $CSS_DIR		= $HTML_BASE . '/css';		# url
our $STATIC_IMG_DIR	= $HTML_BASE . '/images';	# url
our $DYNAMIC_IMG_DIR	= $HTML_BASE . '/dynamic';	# path
our $PATH_IMG_DIR	= $DYNAMIC_IMG_DIR . '/paths';	# url
our $GRID_IMG_DIR	= $STATIC_IMG_DIR . '/grid';	# url

# where binary input files are stored
our $DATA_DIR		= 'data';				# path
# locations of the binary files that contain the graph of paths
our $POINT_FILE		= $DATA_DIR . '/binPointData.dat';	# path
our $LOCATION_FILE	= $DATA_DIR . '/binLocationData.dat';	# path
our $EDGE_FILE		= $DATA_DIR . '/binEdgeData.dat';	# path

# where the thumbnail base image is
our $THUMB_FILE = 'thumbnail.gd2';	# path

# -----------------------------------------------------------------
# Graphics.
# -----------------------------------------------------------------
# how many pixels (on the base map) equal one $UNIT
our $PIXELS_PER_UNIT = 3894;
# an abbreviation of the unit we're using (m for meters, mi for miles, etc)
our $UNITS = "mi";

# this is the scale we use when we're zoomed to a single
# location (a real one). also an index into @SCALES.
our $SINGLE_LOC_SCALE = 1;

# how wide the font that we use for drawling location names is, in pixels.
# Used inside MapGraphics.pm.
our $FONT_WIDTH = 7;
our $FONT_HEIGHT = 13;

# maximum number of characters of a location's name to display in the
# drop-down menu
our $MAX_NAME_LEN = 30;

# how thick are the paths we draw between locations? (in pixels)
our $PATH_THICKNESS = 4;

# -----------------------------------------------------------------
# Graphics::Colors
# -----------------------------------------------------------------
# these are triplets of values between 0 and 255 (inclusive)
# color associated with 'from' location
our @SRC_COLOR = (0, 255, 0);
# color associated with 'to' location
our @DST_COLOR = (255, 0, 0);
# color paths are drawn in
our @PATH_COLOR = (51, 51, 204);
# color the viewport outline rectangle (in the thumbnail) is drawn in
our @RECT_COLOR = (0, 0, 255);
# color location background text (@SRC_COLOR and @DST_COLOR) is drawn in
our @LOC_BG_COLOR = (100, 100, 100);

# -----------------------------------------------------------------
# Caching
# -----------------------------------------------------------------
# where cache files (which store the coordinates of shortest paths) go
our $CACHE_DIR		= 'cache';
# how long cache files hang around for, in seconds
our $CACHE_EXPIRY	= 10*60;

# the suffix of all dynamically-generated images; used for matching for
# deletion
our $DYNAMIC_IMG_SUFFIX	= '.png';
# maximum age, in seconds, of dynamically generated images
our $DYNAMIC_MAX_AGE	= 10*60;
# how long path images stay up
our $PATH_MAX_AGE	= 10*60;

# -----------------------------------------------------------------
# Functions
# -----------------------------------------------------------------
my $_logfh;

###################################################################
# Given a scale, return the path to the GD2 base image at that scale.
# Does NOT check if the file exists.
# Args:
#	- the name of the map, as passed in by the user (a key into %MAPS)
#	- the scale, as an array offset
# Returns:
#	- the path to the GD2 base image at that scale. File might
#	  not exist.
###################################################################
sub getGd2Filename{
	my ($map, $scale) = (@_);
	return $_BASE_NAME . '-' . $MAPS{$map} . '-' . $scale . '.gd2';
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
	return $CACHE_DIR . '/' . min($from, $to) . '-' . max($from, $to) . '.path';
}

###################################################################
# Given a GraphPoint ID, return the name of the cache file that would store the
# result of Dijkstra's algorithm with that GraphPoint at the center.
#
#  Args:
#	- a GraphPoint ID
# Returns:
#	- the name of the Dijkstra cache file for the given GraphPoint
###################################################################
sub getDijkstraCacheName{
	my $id = asInt(shift());
	return "$CACHE_DIR/$id.full";
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
	return $min if($val < $min);
	return $max if($val > $max);
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
	return( ($x < $y) ? $x : $y );
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
	return( ($x > $y) ? $x : $y );
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
	return( $r || 0 );
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
	return( int($n + 0.5 * ($n <=> 0)) );
}

###################################################################
# Get the word-character ([a-zA-Z0-9_]) part of a string, which must start at
# the beginning. If no such string exists, the empty string is returned.
# Args:
#	- the string from which to extract word characters
###################################################################
sub getWords{
	my($str) = shift;
	$str =~ /^(\w+)/;
	return $1 || '';
}

###################################################################
# Print to the log file. Messages printed directly by the system, not through
# plog(), should be prepended with "=== ".
# EITHER:
#	- the string to print
# OR:
#	- the log level
#	- a string to print
#	- [...]
#
# E.g., these are good:
# plog("Foo!");
# plog(1, "Foo");
# plog(1, "Foo", "Bar", "baz");
#
# But these are not:
# plog(1);
# plog("Foo", "Bar");
#
###################################################################
sub plog{
	# log level of the message defaults to 1, or the first argument
	# if more than one was passed
	my $level = 1;
	if(@_ > 1){
		my $level = int shift;
	}

	# logging must be at least the level of the message, or nothing happens
	if($LOG_LEVEL >= $level){
		# open the log file if it's not open already
		if(! defined($_log_fh) ){
			open($_log_fh, '>>', $LOG_FILE);

			# set $_log_fh to be unbuffered
			my $prev_fh = select($_log_fh);
			$| = 1;
			select($prev_fh);

			print $_log_fh "=== Opened log file at " . scalar localtime() . "\n";
		}
		print  $_log_fh (@_);
	}
}

###################################################################
# Any cleanup for this module.
###################################################################
END{
	if( defined($_log_fh) ){
		print $_log_fh "=== Closed log file at " . scalar localtime() . "\n";
		close($_log_fh);
	}
}

1;
