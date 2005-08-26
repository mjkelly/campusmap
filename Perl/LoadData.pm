# -----------------------------------------------------------------
# LoadData.pm -- Load binary data output from PathOptimize.java into
# into Perl data structures for manipulation.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Wed Aug 24 20:45:19 PDT 2005
# -----------------------------------------------------------------

package LoadData;

require Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(nameNormalize tokenize findLocation findKeyword isKeyword getKeyText);
@EXPORT = qw();


use strict;
use warnings;
use Text::WagnerFischer qw(distance);
use MapGlobals qw(TRUE FALSE INFINITY);
use Heap::Elem::GraphPoint;
use Fcntl qw(:seek);

use constant {
	INT => 4,	# the size of an integer, in bytes
	BYTE => 1,	# the size of a byte, in bytes ;)
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

	# hash we build up for each new point
	# loop while we can read an ID from disk (terminate on EOF)
	while( defined(my $ID = readInt(*INPUT)) ){
		print STDERR "Read point ID $ID\n" if DEBUG;

		# this is where we put all the values we read off disk
		my %newpt = ();

		# set its 'ID' attribute
		$newpt{'ID'} = $ID;

		# just in case we want coords without having to cycle through
		# connections
		$newpt{'x'} = readInt(*INPUT);
		$newpt{'y'} = readInt(*INPUT);

		# get the number of connections
		my $conns = readInt(*INPUT);
		print STDERR "$conns connections.\n" if DEBUG;

		# make 'Connections' an array in the current point object
		$newpt{'Connections'} = {};

		$newpt{'ConnectionsArray'} = [];

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
			if( !exists($newpt{'Connections'}{$connID})
				|| $newpt{'Connections'}{$connID}{'Weight'} > $weight)
			{
				$newpt{'Connections'}{$connID} = {
					ConnectionID => $connID,
					Weight => $weight,
					EdgeID => $edgeID,
				};
			}

			# then, we add a reference to this connection to an
			# array of connections
			push( @{$newpt{'ConnectionsArray'}},
				$newpt{'Connections'}{$connID});
		}

		# read the location ID
		$newpt{'LocationID'} = readInt(*INPUT);
		# and any binary flags
		$newpt{'PassThrough'} = readByte(*INPUT);

		# now we initialize fields for shortest-path calculations
		$newpt{'Known'} = FALSE;
		$newpt{'Distance'} = INFINITY;
		$newpt{'From'} = undef;

		print STDERR "Location ID: $newpt{'LocationID'}\n" if DEBUG;
		print STDERR "---end---\n" if DEBUG;

		# finally, we stick all of this into our $points hashref
		$points->{$ID} = Heap::Elem::GraphPoint->new(%newpt);
		
	}

	close(INPUT);

	# return the resultant hash of points
	return $points;
}

###################################################################
# Load locations from a binary disk file.
# Locations are hashed both by numeric ID, by normalized name, and possibly by building code.
#
# To get by ID: $locations->{'ByID'}{$ID}
# To get by name: $locations->{'ByName'}{nameNormalize($name)}
# To get by building code: $locations->{'ByCode'}{$bldngCode}
#
# Remember this when looping through all location keys.
#
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
	my $locations = { ByID => {}, ByName => {}, ByCode => {}, ByKeyword => {} };

	open(INPUT, '<', $filename) or die "Cannot open $filename for reading: $!\n";

	# read until EOF
	while( defined(my $ID = readInt(*INPUT)) ){
		print STDERR "Read Location ID: $ID\n" if DEBUG;

		# store this location by ID. this is the actual hash we use to
		# store all the data -- the ByName and ByCode hashes refer to
		# here.
		$locations->{'ByID'}{$ID} = {};
		my $thisLoc = $locations->{'ByID'}{$ID};

		# set this location's ID
		$thisLoc->{'ID'} = $ID;

		# get (x,y) coordinates
		$thisLoc->{'x'} = readInt(*INPUT);
		$thisLoc->{'y'} = readInt(*INPUT);
		print STDERR "Location coords: "
			. "($thisLoc->{'x'}, $thisLoc->{'y'})\n" if DEBUG;

		# read the boolean flags
		$thisLoc->{'DisplayName'} = readByte(*INPUT);

		# get the associated GraphPoint's ID
		$thisLoc->{'PointID'} = readInt(*INPUT);
		print STDERR "Point ID: $thisLoc->{'PointID'}\n" if DEBUG;

		# read in the Location's name string, knowing that its length 
		# is right in front of the actual character data
		my $name = readJavaString(*INPUT);
		$thisLoc->{'Name'} = $name;
		print STDERR "Name: $thisLoc->{'Name'}\n" if DEBUG;

		# now add the location under its name hash
		$locations->{'ByName'}{nameNormalize($name)} = $thisLoc;

		# read the location's building code
		my $code = readJavaString(*INPUT);
		$thisLoc->{'BuildingCode'} = $code;
		print STDERR "Building Code: $thisLoc->{'BuildingCode'}\n" if DEBUG;

		# store the location under its building code as well
		# (this means all building codes must be unique! ...obviously)
		if( length($code) ){
			$locations->{'ByCode'}{$code} = $thisLoc;
		}
		
		# read the location's keyword field
		$thisLoc->{'Keywords'} = readJavaString(*INPUT);
		print STDERR "Keywords: $thisLoc->{'Keywords'}\n" if DEBUG;

		# add this location under each of its keywords
		foreach my $key (tokenize($thisLoc->{'Keywords'})){
			push(@{$locations->{'ByKeyword'}{$key}}, $thisLoc);
		}

		# read the location's aliases, one at a time
		my $numAliases = readInt(*INPUT);
		@{$thisLoc->{'Aliases'}} = ();
		for(1..$numAliases){
			push(@{$thisLoc->{'Aliases'}}, readJavaString(*INPUT));
		}

		print STDERR "Storing under " . nameNormalize($name) . "\n" if DEBUG;
		print STDERR "---end---\n" if DEBUG;
	}

	close(INPUT);

	return $locations;
}

###################################################################
# read an network-formatted integer from the specified filehandle
# Args:
#	- a typeglob specifying the filehandle to read from
# Returns:
#	- the read data, as an int, or undef if the read() failed
###################################################################
sub readInt{
	my $buf;
	if(! read(shift, $buf, INT) ){
		return undef;
	}
	return unpack("N", $buf);
}

###################################################################
# write the given integer ito the given filehandle as a network-order
# (big-endian) long.
# Args:
#	- the filehandle to write to
#	- the integer to write
###################################################################
sub writeInt{
	my($fh, $i) = @_;
	print $fh pack("N", $i);
}

###################################################################
# read one byte from the specified filehandle.
# Args:
#	- a typeglob specifying the filehandle to read from
# Returns:
#	- the read byte, or undef if the read() failed
###################################################################
sub readByte{
	my $buf;
	if(! read(shift(), $buf, BYTE) ){
		return undef;
	}
	# since it's only one byte, we don't have to worry about network byte order
	return ord($buf);
}

###################################################################
# Load an edge of a given ID from the edge file. Objects are of 
# constant length, so it's a simple matter of seeking to the
# correct spot in the file.
#
# Args:
#	- the filehandle to the edge file
#	- the size of each edge object on disk
#	- the ID of the edge file to load
###################################################################
sub loadEdge{
	my($fh, $size, $id) = @_;

	my $offset = INT + ($size*($id-1));
	print STDERR "Loading Edge ID $id. Edge size is $size. Seeking to: $offset\n" if DEBUG;
	seek($fh, $offset, SEEK_SET);
	
	# initialize a new hash to hold this Edge
	my $edge = {};
	my $ID = readInt($fh);
	$edge->{'ID'} = $ID;
	print STDERR "----\n" if DEBUG;
	print STDERR "Edge ID: $ID\n" if DEBUG;

	# the IDs of the GraphPoints at the start and end of this Edge
	$edge->{'StartPoint'} = readInt($fh);
	$edge->{'EndPoint'} = readInt($fh);
	print STDERR "StartPoint ID: $edge->{'StartPoint'}\n" if DEBUG;
	print STDERR "EndPoint ID: $edge->{'EndPoint'}\n" if DEBUG;

	my $numPoints = readInt($fh);
	print STDERR "Number of points: $numPoints\n" if DEBUG;

	# initialize this Edge's path to an empty array
	$edge->{'Path'} = ();
	for my $i (1..$numPoints){
		# read in (x,y) coordinates
		my $x = readInt($fh);
		my $y = readInt($fh);

		print STDERR "Path Point: ($x, $y)\n" if DEBUG;

		# add those coordinates to this Edge's path
		push(@{$edge->{'Path'}}, {
			x => $x,
			y => $y,
		});
	}
	print STDERR "---end---\n" if DEBUG;

	return $edge;
}

###################################################################
# open a file handle to the edges file, and return the size
# of each edge object (which is constant for all edges).
# Args:
#	- filename of the edge file
# Returns:
#	- filehandle to the opened file
#	- the (constant) size of each edge object in the file
###################################################################
sub initEdgeFile{
	my($filename) = @_;
	my $fh;

	open($fh, '<', $filename) or die "Cannot open edge file $filename: $!\n";
	my $size = readInt($fh);

	return($fh, $size);
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
	my($fh) = @_;
	my $buf;

	# each character is two bytes, plus the size of an int at the very beginning,
	# which specifies the length of the string

	# first we get the length of the string
	my $len = readInt($fh);

	# now read in the rest of the string, according to its length
	# (java chars are 2 bytes long)
	read($fh, $buf, ($len*2));

	# unpack $buf as a series of ascii characters
	my $str = unpack("a*", $buf);

	# remove all the nulls: Java strings are two bytes, but ascii
	# characters are only one, so we've got a null between each character.
	$str =~ s/\0//g;

	return $str;
}

###################################################################
# Write a set of points representing the shortest path between two locations to
# a cache file, for quick retrieval (without running Dijkstra's algorithm)
# later.
# TODO: add detailed description of file format.
#
# Args:
#	- the filename to write to
#	- the distance between the two points, in pixels
#	- the viewing rectangle: that is, the minimum and maximum x and y
#	  coordinates of the points making up the path (yes, we could calculate
#	  these, but it's only 16 bytes)
#	- an arrayref containing arrayrefs of points (which are hashes with 'x'
#	  and 'y' keys). Each sub-arrayref represents an Edge object.
# Returns: n/a
###################################################################
sub writeCache{
	my ($file, $dist, $rect, $pathPoints) = @_;

	# dist is 0 if it's undefined
	$dist ||= 0;

	print STDERR "WRITING TO CACHE...\n" if DEBUG;
	open(CACHE, '>', $file) or die "Cannot open cache file $file for writing: $!\n";
	# the distance of the path
	print STDERR "distance: $dist\n" if DEBUG;
	writeInt( *CACHE, $dist );

	# the corners of the bounding rectangle around the path
	print STDERR "bounding rectangle: ($rect->{'xmin'}, $rect->{'ymin'}) - ($rect->{'xmax'}, $rect->{'ymax'})\n" if DEBUG;
	writeInt( *CACHE, $rect->{'xmin'} );
	writeInt( *CACHE, $rect->{'ymin'} );
	writeInt( *CACHE, $rect->{'xmax'} );
	writeInt( *CACHE, $rect->{'ymax'} );

	# the number of point pairs in the file
	print STDERR "number of points: " . scalar(@$pathPoints) . "\n" if DEBUG;
	writeInt( *CACHE, scalar(@$pathPoints) );

	# the actual path coordinates
	foreach my $subpath (@$pathPoints){
		# how long the subpath is
		print "SUBPATH: " . scalar(@$subpath) . "\n" if DEBUG;
		writeInt( *CACHE, scalar(@$subpath) );

		# each coordinate in the subpath
		foreach (@$subpath){
			print STDERR "           ($_->{'x'}, $_->{'y'})\n" if DEBUG;
			writeInt( *CACHE, $_->{'x'} );
			writeInt( *CACHE, $_->{'y'} );
		}
	}
	close(CACHE);
}

###################################################################
# Load a given file as a cache of the points on the shortest path between two
# locations. The file is in a binary format described by writeCache().
# Args:
#	- the name of the cache file to load
# Returns:
#	- the same as ShortestPath::pathPoints().
###################################################################
sub loadCache{
	my ($file) = @_;

	my $now = time();
	print STDERR "LOADING FROM CACHE...\n" if DEBUG;
	# update the modification and access times. this is important, becuse
	# cacheReaper() checks _modification_ time, not access time. this is a
	# stupid hack to get around systems that may have 'noatime' set (such
	# as Gentoo machines, by default).
	utime($now, $now, $file);

	open(CACHE, '<', $file) or die "Cannot open cache file $file for reading: $!\n";
	# the distance of the path
	my $dist = readInt(*CACHE);
	# undef is converted to 0 in writeCache(), so we need to
	# convert it back
	if($dist == 0){
		$dist = undef;
	}

	# the corners of the bounding rectangle around the path
	my %rect = ();
	$rect{'xmin'} = readInt(*CACHE);
	$rect{'ymin'} = readInt(*CACHE);
	$rect{'xmax'} = readInt(*CACHE);
	$rect{'ymax'} = readInt(*CACHE);
	print STDERR "bounding rectangle: ($rect{'xmin'}, $rect{'ymin'}) - ($rect{'xmax'}, $rect{'ymax'})\n" if DEBUG;

	# the number of point pairs in the file
	my $numPoints = readInt(*CACHE);
	print STDERR "number of points: $numPoints\n" if DEBUG;

	my @points;
	my ($x, $y);
	my $sublength = 0;
	for(1..$numPoints){
		my $subpoints = [];
		# read in the length of the subpath
		$sublength = readInt(*CACHE);
		# read in each ordered pair in this subpath
		for(1..$sublength){
			$x = readInt(*CACHE);
			$y = readInt(*CACHE);
			print STDERR "\t($x, $y)\n" if DEBUG;
			push(@$subpoints, { x => $x, y => $y });
		}

		push(@points, $subpoints);
	}

	close(CACHE);
	return($dist, \%rect, \@points);
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
# Given a search string and a hashref of locations (returned by
# MapGlobals::loadLocations()), find the best match, or return a list of
# possible matches if no single sufficiently good match exists.
#
# Args:
#	- the search string
#	- a hashref of locations
# Returns:
#	- an array of hashrefs in the same format as fuzzyFind.
###################################################################
sub findLocation{
	my($text, $locations) = @_;

	if($text eq ''){
		return ();
	}

	my $norm_text = nameNormalize($text);

	# first check for an exact name match
	if( exists($locations->{'ByName'}{$norm_text}) ){
		return ({
			id => $locations->{'ByName'}{$norm_text}{'ID'},
			matches => 1.0,
			text => $locations->{'ByName'}{$norm_text}{'Name'},
		});
	}
	# then check a building code match
	elsif( exists($locations->{'ByCode'}{$text}) ){
		return ({
			id => $locations->{'ByCode'}{$text}{'ID'},
			matches => 1.0,
			text => $locations->{'ByCode'}{$text}{'Name'},
		});
	}
	# otherwise, fall back to substrings and fuzzy things
	elsif($text ne ''){
		return LoadData::fuzzyFind($text, $locations);
	}
}

###################################################################
# Given user input, return the ID of the location that best matches, or -1 if
# there's nothing reasonable.
#
# TODO: This code is messy. Make it less so, sometime.
#
# Args:
#	- a search string
#	- a hashref of locations to search
# Returns:
#	- an array containing the best matches: each element in the array is a
#	hashref containing the keys 'id' (the ID of the location), 'matches' (a
#	floating-point number representing the relative goodness of a match),
#	and 'text', which is the precise text that was matched -- this may be
#	the official location name or an alias.
#	The array is sorted by the 'matches' field. i.e., 
#	  (
#		{ id => 6, matches => 0.25, text => 'Foo' },
#		{ id => 42, matches => 0.20, text => 'Bar' },
#		{ id => 17, matches => 0.15, text => 'Baz' }
#	  )
###################################################################
sub fuzzyFind{
	my ($search_str, $locations) = @_;
	my @search_toks = tokenize($search_str);
	my $search_norm = nameNormalize($search_str);
	my $search_len = scalar @search_toks;

	# trace function execution, for debugging
	my $outstr = '';

	# store the top few matches we get.
	my @top_matches;

	#warn "SEACH STRING: $search_str -> (@search_toks) [$search_len]\n";

	# first look for straight substrings
	my $best_id = undef;
	my $best_txt = undef;
	#warn "SUBSTRING LOOP:\n";
	foreach my $loc_id ( keys %{$locations->{'ByID'}} ){
		#warn "$locations->{'ByName'}{$name}{'Name'}\n";

		#warn "\tID = $loc_id\n";

		# search through the primary name, and all aliases
		foreach( $locations->{'ByID'}{$loc_id}{'Name'}, @{$locations->{'ByID'}{$loc_id}{'Aliases'}} ){
			my $norm = nameNormalize($_);
			#warn "\t\t$_ --> $norm\n";
			#warn "\t$norm\n";
			# search for simple substrings
			if( index($norm, $search_norm) != -1 ){
				#warn "*** $norm is a superstring of $search_norm.\n";
				# only keep the shortest substring
				if( !defined($best_txt) || length($_) < length($best_txt) ){
					#warn "*** this is the best substring.\n";
					#warn "*** it represents $locations->{'ByName'}{$name}{'ID'}\n";
					$best_id = $loc_id;
					$best_txt = $_;
				}
			}
		}
	}

	# if we got a substring, just return that -- don't bother with expensive fuzzy matching
	if( defined($best_txt) ){
		#warn "returning id = $locations->{'ByID'}{$best_id}{'ID'}\n";
		return({
			id => $locations->{'ByID'}{$best_id}{'ID'},
			matches => 1.0,
			text => $best_txt,
		});
	}
	#warn "aww, we fell through. time for fuzzy matching.\n";

	#warn "FUZZY LOOP:\n";
	foreach my $loc_id ( keys %{$locations->{'ByID'}} ){
		# this is just an array made up of the location's primary name
		# and all its aliases
		#warn "\tID = $loc_id\n";
		foreach( $locations->{'ByID'}{$loc_id}{'Name'}, @{$locations->{'ByID'}{$loc_id}{'Aliases'}} ){
			my @loc_toks = tokenize($_);
			#warn "\t\t$_ --> (@loc_toks)\n";
			my $loc_len = scalar @loc_toks;
			$outstr .= "LOCATION: $_ -> (@loc_toks) [$loc_len]\n";

			# aaaagh, nested loops...
			my $matches = 0;
			my $l_matched = 0;
	LOC:			foreach my $l_tok (@loc_toks){
				$outstr .= "\t$l_tok:";
	SEARCH:				foreach my $s_tok (@search_toks){
					# exact match
					if($s_tok eq $l_tok){
						my $strength = 1;
						$outstr .= " $s_tok [$strength EXACT]\n";

						$matches += $strength;
						$l_matched++;
						# we found a perfect match for this token.
						# move on to the next token.
						next LOC;
					}
					# substring search
					elsif( index($l_tok, $s_tok) != -1 ){
						my $strength = length($s_tok) / length($l_tok);
						$outstr .= " $s_tok [$strength SUB]";

						$matches += $strength;
						$l_matched++;
						next SEARCH;
					}
					# superstring search
					elsif( index($s_tok, $l_tok) != -1 ){
						my $strength = length($l_tok) / length($s_tok);
						$outstr .= " $s_tok [$strength SUPER]";

						$matches += $strength;
						$l_matched++;
						next SEARCH;
					}
					# fuzzy matching...
					else{
						my $dist = distance([0, 1, 1], $s_tok, $l_tok);
						# the "weighted distance" is
						# ($dist/ length($s_tok). the higher
						# this is, the worse the match.
						my $strength = 1 - ($dist/ length($s_tok));

						# ignore the really bad matches
						# this is critical, so long names
						# don't accumulate match strength
						# from a series of bad matches
						if($strength > 0.5){
							$matches += $strength;
							$l_matched++;
							$outstr .= " $s_tok [$dist -> $strength FUZZY]";
							next SEARCH;
						}
					}
				}
				$outstr .= "\n";
			}

			# if we had any matches, keep track of this result
			if($matches && $l_matched){
				# we heavily weight in favor of multiple word matches,
				# but lightly weight against longer matches.
				# In other words, if you search for "foo bar",
				# the following strings should match in the
				# following order:
				# 1. "foo bar"
				# 2. "foo bar baz"
				# 3. "foo baz"
				# All of this is subject to change, of course. ;)
				$matches = ($matches*$matches) / ($loc_len);
				if($matches >= 0.05){
					push(@top_matches, {
						id => $loc_id,
						matches => $matches,
						text => $_,
					});
				}
			}

			$outstr .= "\t$matches matches ($l_matched).\n";
		}
	}
	#warn "$outstr";

	# this is the fifth or last index
	my $four = $#top_matches < 4 ? $#top_matches : 4;

	# now we decide how many matches to return
	if(@top_matches){
		@top_matches = sort { $b->{'matches'} <=> $a->{'matches'} } @top_matches;
		#warn "MATCHES:\n";
		#warn "\t$locations->{'ByID'}{$_->{'id'}}{'Name'} [$_->{'matches'}]\n" for @top_matches;

		# if it's a high enough score...
		if( $top_matches[0]{'matches'} > 0.5 ){
			return $top_matches[0];
		}
		# we've probably got a variety of crappy matches to choose from.
		# return the top 5.
		else{
			return @top_matches[0..$four];
		}
	}
	# there were no matches at all. return nothing.
	else{
		return ();
	}
}

###################################################################
# Tokenize a given string: return an array containing the normalized version of
# each word of the string. Extremely common or otherwise insignificant words
# are removed.
# Args:
#	- the string to tokenize
# Returns:
#	- an array containing all the tokens
###################################################################
sub tokenize{
	my($str) = @_;

	# internal punctuation is gone
	$str =~ s/(\w)'(\w)/$1$2/g;

	# split on whitespace
	my @toks = split(/[\s\/]+/, $str);

	# normalize each chunk, but don't transfer any values
	# that are normalized away
	my %newtoks;
	my $norm_str;
	foreach (@toks){
		$norm_str = nameNormalize($_);
		next if( $norm_str eq '' );
		next if( $norm_str =~ /^(and|of|by|for)$/ );

		$newtoks{$norm_str} = 1;
	}

	# return the whole thing
	return keys %newtoks;
}

###################################################################
# Find all the locations with a given keyword. The order in which they are
# returned is not specified.
#
# Args:
#	- the keyword. this should already be stripped of the 'keyword:'
#	  prefix, and properly normalized.
# Returns:
#	- an array containing the matches, in a similar format to findLocation():
#	  (
#		{ id => 6, matches => 1 },
#		{ id => 42, matches => 1 },
#		{ id => 17, matches => 1 }
#	  )
# 	Note that the 'matches' key is always 1, because it means nothing here.
###################################################################
sub findKeyword{
	my($keyword, $locations) = @_;
	my @r = ();
	foreach my $l ( @{$locations->{'ByKeyword'}{$keyword}} ){
		push(@r, { id => $l->{'ID'}, matches => 1 });
	}
	return @r;
}

###################################################################
# Check if a the given search string represents a keyword search.
# Args:
#	- a search string
# Returns:
#	- 1 if the given search string is a keyword search, else 0
###################################################################
sub isKeyword{
	my($str) = @_;
	return (lc(substr($str, 0, 8)) eq 'keyword:');
}

###################################################################
# Return the formatted keyword text from a search string.
# Args:
#	- the search string
# Returns:
#	- 
###################################################################
sub getKeyText{
	my $str = shift();
	return nameNormalize(substr($str, 8));
}

1;

