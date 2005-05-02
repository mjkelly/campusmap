# -----------------------------------------------------------------
# LoadData.pm -- Load binary data output from PathOptimize.java into
# into Perl data structures for manipulation.
#
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Thu Mar 24 13:50:31 PST 2005
# -----------------------------------------------------------------

package LoadData;

use strict;
use warnings;
use MapGlobals;

use constant {
	INT => 4,	# the size of an integer, in bytes
	DEBUG => 0,	# whether to print lots of debugging info when reading
};

###################################################################
# Load GraphPoints from a binary disk file.
# This also adds fields to the GraphPoints that are not represented on disk,
# which are needed for calculating shortest paths.
# Args:
#	- the name of the file to load from
# Returns:
#	- a hashref containing all the data read
#	  (see LoadData.pl for an example of traversing this data structure)
###################################################################
sub loadPoints{
	my($filename) = @_;

	# buffer for input
	my $buf;

	# value of unpacked variables
	my $unpacked;

	# hashref of points, to return
	my $points = {};

	open(INPUT, '<', $filename) or die "Cannot open $filename for reading: $!\n";

	# loop while we can read an ID from disk (terminate on EOF)
	while( defined(my $ID = readInt(*INPUT)) ){
		print STDERR "Read point ID $ID\n" if DEBUG;

		# create a sub-hash in %points to store this point object
		$points->{$ID} = {};

		# set its 'ID' attribute
		# XXX: take this out later?
		$points->{$ID}{'ID'} = $ID;

		# get the number of connections
		my $conns = readInt(*INPUT);
		print STDERR "$conns connections.\n" if DEBUG;

		# make 'Connections' an array in the current point object
		$points->{$ID}{'Connections'} = {};

		$points->{$ID}{'ConnectionsArray'} = [];

		# loop as many times as there are connections
		for my $i (1..$conns){
			print STDERR "---Start connection---\n" if DEBUG;
			
			# read connection ID
			my $connID = readInt(*INPUT);

			# read weight
			my $weight = readInt(*INPUT);

			# read edge ID
			my $edgeID = readInt(*INPUT);

			print STDERR "Connection ID: $connID\n" if DEBUG;
			print STDERR "Weight: $weight\n" if DEBUG;
			print STDERR "Edge ID: $edgeID\n" if DEBUG;

			# put all these elements (connection ID, weight, edge ID) into
			# a hash, which in turn is stored in another hash by connection ID;
			# if we have a collision, we store the one with the lower weight
			# (collisions mean there are two paths between two given points, and
			# we'll NEVER, in our shortest-path algorithms, want to take the
			# longer one. we only keep it for completeness.)
			if( !exists($points->{$ID}{'Connections'}{$connID})
				|| $points->{$ID}{'Connections'}{$connID}{'Weight'} > $weight)
			{
				$points->{$ID}{'Connections'}{$connID} = {
					# XXX: take this out later?
					ConnectionID => $connID,
					Weight => $weight,
					EdgeID => $edgeID,
				};
			}

			# then, we add a reference to this connection to an
			# array of connections
			push( @{$points->{$ID}{'ConnectionsArray'}},
				$points->{$ID}{'Connections'}{$connID});
		}

		# read the location ID
		$points->{$ID}{'LocationID'} = readInt(*INPUT);

		# now we initialize fields for shortest-path calculations
		$points->{$ID}{'Known'} = FALSE;
		$points->{$ID}{'Distance'} = INFINITY;
		$points->{$ID}{'From'} = undef;

		print STDERR "Location ID: $points->{$ID}{'LocationID'}\n" if DEBUG;
		print STDERR "---end---\n" if DEBUG;
	}

	close(INPUT);

	# return the resultant hash of points
	return $points;
}

###################################################################
# Load locations from a binary disk file.
# Locations are hashed both by numeric ID and by name. Prepend "name:"
# to the actual name of the location to look up.
# Args:
#	- the name of the file to load from
# Returns:
#	- a hashref containing all the data read
#	  (see LoadData.pl for an example of traversing this data structure)
###################################################################
sub loadLocations{
	my($filename) = @_;

	my $buf;
	my $unpacked;

	# a hashref of locations to return
	my $locations = {};

	open(INPUT, '<', $filename) or die "Cannot open $filename for reading: $!\n";

	# read until EOF
	while( defined(my $ID = readInt(*INPUT)) ){
		print STDERR "Read Location ID: $ID\n" if DEBUG;

		# create a sub-hash for this location
		$locations->{$ID} = {};

		# set this location's ID
		# XXX: take this out later?
		$locations->{$ID}{'ID'} = $ID;

		# get (x,y) coordinates
		$locations->{$ID}{'x'} = readInt(*INPUT);
		$locations->{$ID}{'y'} = readInt(*INPUT);
		print STDERR "Location coords: "
			. "($locations->{$ID}{'x'}, $locations->{$ID}{'y'})\n" if DEBUG;

		# get the associated GraphPoint's ID
		$locations->{$ID}{'PointID'} = readInt(*INPUT);
		print STDERR "Point ID: $locations->{$ID}{'PointID'}\n" if DEBUG;

		# read in the Location's name string, knowing that its length 
		# is right in front of the actual character data
		my $name = readJavaString(*INPUT);
		$name =~ s/\0//g;
		$locations->{$ID}{'Name'} = $name;
		print STDERR "Name: $locations->{$ID}{'Name'}\n" if DEBUG;

		# now add the location under its name hash
		$locations->{'name:' . nameNormalize($name)} = $locations->{$ID};

		print STDERR "Storing under " . nameNormalize($name) . "\n" if DEBUG;
		print STDERR "---end---\n" if DEBUG;
	}

	close(INPUT);

	return $locations;
}

###################################################################
# Read Edges from a binary disk file.
# Args:
#	- the name of the file to load from
# Returns:
#	- a hashref containing all the data read
#	  (see LoadData.pl for an example of traversing this data structure)
###################################################################
sub loadEdges{
	my($filename) = @_;

	my $buf;

	# a hashref containing all the read Edges, indexed by ID, to return
	my $edges = {};

	open(INPUT, '<', $filename) or die "Cannot open $filename for reading: $!\n";

	# read until EOF
	while( defined(my $ID = readInt(*INPUT)) ){
		# initialize a new hash to hold this Edge
		$edges->{$ID} = {};

		# initialize
		# XXX: take this out later?
		$edges->{$ID}{'ID'} = $ID;
		print STDERR "----\n" if DEBUG;
		print STDERR "Edge ID: $ID\n" if DEBUG;

		# the IDs of the GraphPoints at the start and end of this Edge
		$edges->{$ID}{'StartPoint'} = readInt(*INPUT);
		$edges->{$ID}{'EndPoint'} = readInt(*INPUT);
		print STDERR "StartPoint ID: $edges->{$ID}{'StartPoint'}\n" if DEBUG;
		print STDERR "EndPoint ID: $edges->{$ID}{'EndPoint'}\n" if DEBUG;

		my $numPoints = readInt(*INPUT);

		# initialize this Edge's path to an empty array
		$edges->{$ID}{'Path'} = ();
		for my $i (1..$numPoints){
			# read in (x,y) coordinates
			my $x = readInt(*INPUT);
			my $y = readInt(*INPUT);

			print STDERR "Path Point: ($x, $y)\n" if DEBUG;

			# add those coordinates to this Edge's path
			push(@{$edges->{$ID}{'Path'}}, {
				x => $x,
				y => $y,
			});
		}
		print STDERR "---end---\n" if DEBUG;
	}

	close(INPUT);

	return $edges;
}

###################################################################
# read an network-formatted integer from the specified filehandle
# Args:
#	- a typeglob specifying the filehandle to read from
# Returns:
#	- the read data, as an int, or undef if the read() failed
###################################################################
sub readInt{
	if(!@_){
		warn "readInt() requires an argument!\n";
		return;
	}

	my $fh = shift(@_);
	my $buf;

	if(! read($fh, $buf, INT) ){
		return undef;
	}

	return asInt($buf);
}

###################################################################
# unpack a native-format int, given
# Args: 
#	- a scalar holding the data to decode
# Returns:
#	- the unpacked data, as a Perl int
###################################################################
sub asInt{
	if(!@_){
		warn "asInt() requires an argument!\n";
		return;
	}
	return unpack("N", shift(@_));
}


###################################################################
# Read a Java character array (not a String object) from the 
# specified filehandle. The current location in the file should be
# an integer, specifying the length of the string, immediately followed
# by the string characters.
# Args:
#	- the filehandle to read from
# Returns:
# 	- the string read
###################################################################
sub readJavaString{
	if(!@_){
		warn "readJavaString() requires two arguments!\n";
		return;
	}
	my($fh) = @_;
	my $buf;

	# each character is two bytes, plus the size of an int at the very beginning,
	# which specifies the length of the string

	# first we get the length of the string
	my $len = readInt($fh);

	# now read in the rest of the string, according to its length
	read($fh, $buf, ($len*2));

	# now unpack the string, using the length and data we read before
	return unpack("n/a*", $len . $buf);
}

###################################################################
# Normalize a Location name string to make subsequent searching easier.
# Normalization consists of all non-alphanumerics, and lowercasing
# all letters.
# Args:
#	- the name to normalize, as a string
# Returns:
#	- the normalized name
###################################################################
sub nameNormalize{
	my $name = shift;

	$name = lc($name);
	$name =~ s/\W//g;

	return $name;
}

###################################################################
# Return the "lookup name" of a location, given a plaintext location name. This
# "lookup name" constists of the string "name:" followed by the normalized
# location name, and can be put into a locations hashref to get a location object
# by plaintext name.
# Args:
#	- the string whose "lookup name" is needed
# Returns:
#	- the "lookup name" of the given string
###################################################################
sub nameLookup{
	my $name = shift;
	return 'name:' . nameNormalize($name);
}

###################################################################
# Given user input, return the ID of the location that best matches, or -1 if
# there's nothing reasonable.
#
# Args:
#	- a search string
#	- a hashref of locations to search
# Returns:
#	- the ID of the best-matching location, or -1 if there is none
###################################################################
sub findName{
	my ($name, $locations) = @_;

	# we're not going anywhere without a normalized name
	$name = nameNormalize($name);

	my $match;
	my $loc;
	# loop through all the locations, checking for possible matches
	foreach (keys %$locations){
		if( substr($_, 0, 5) eq 'name:' ){
			$loc = substr($_, 5);
			# check of the location is a superset of the search string
			if( $loc =~ /$name/ ){
				# either this is the first match, or this is SHORTER
				# than the existing one
				if(! defined($match) || length($match) > length($loc) ){
					$match = $loc;
				}
			}
			# check if the search string is a superset of the location
			# (this is less desirable)
			elsif( $name =~ /$loc/ ){
				# either this is the first match, or this is LONGER
				# than the existing one
				if(! defined($match) || length($match) < length($loc) ){
					$match = $loc;
				}
			}
		}
	}

	# get the ID of the match, if one was found
	my $id = -1;
	if(defined($match)){
		$id = $locations->{nameLookup($match)}{'ID'};
	}

	return $id;
}

1;
