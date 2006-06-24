#!/usr/bin/perl
# -----------------------------------------------------------------
# parsell.pl -- $desc$
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Fri Aug 12 13:59:11 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use lib qw(
	../
);

use LoadData qw(tokenize);

require 'll2pix.pm';

# old map
#use constant {
#	MAX_X => 7200,
#	MAX_Y => 6600,
#};

use constant {
	MAX_X => 9568,
	MAX_Y => 8277,
};

use constant{
	ARRAYCMP_EQUAL => 0,
	ARRAYCMP_SUBSET => -1,
	ARRAYCMP_SUPERSET => 1,
	ARRAYCMP_DIFF => 2,
};


#print arrayCmp(['erc', 'life', 'office', 'residence'], ['earth', 'hall']) . "\n";
#exit;

my $infile = "$ENV{'HOME'}/ucsdmap/Google.csv";

open(IN, '<', $infile) or die "Cannot open input file: $!\n";

# throw away first line; it's a key
<IN>;

my $lastID;
my $lastOfficial;
#my @lastToks;
my @names;
my $keep = 1;

# read the rest of the lines
while(<IN>){
	chomp;
	my @parts = split(/"?\t"?/, $_);

	# we always print official names
	if($parts[2] eq 'Official'){

		# the first thing we do is print out all the aliases for the previous location
		shift(@names);
		#print scalar(@names) . " aliases:\n";
		foreach(@names){
			print "ALIAS\t$_\n";
		}
		@names = ();

		my ($x, $y) = ($parts[5], $parts[6]);
			
		# remove quotes (because they're ugly) and tabs (because they corrupt
		# the data format) from the location names
		$parts[1] =~ s/"//g;
		$parts[1] =~ s/\t//g;

		my($px, $py) = ll2pix($x, $y);

		$lastID = $parts[0];

		# make sure the location will be on the map
		if($px >= 0 && $py >= 0 && $px <= MAX_X && $py <= MAX_Y){
			printf("OFFICIAL\t%s\t%.0f\t%.0f\n", $parts[1], $px, $py);
			#@lastToks = sort(tokenize($parts[1]));
			@names = ($parts[1]);
			$keep = 1;
		}
		else{
			#print("DROP $parts[1] $px $py\n");
			$keep = 0;
		}
	}

	# if we run into an alias, check if it's unique before printing it
	elsif($parts[2] eq 'Alias'){
		# make sure this alias's ID refers back to the previous
		# official location's ID
		if($parts[0] eq $lastID){
			next if(!$keep);
			# we know this is a valid alias declaration: now decide
			# if it's important enough to keep

			#print "------> $parts[1]\n";

			my @toks = sort(tokenize($parts[1]));
			#printf("ALIAS %s\n", $parts[1]);

			my $add = 1;
			# check this alias name against all previous names for this location
			foreach my $i (0..$#names){
				my @oldToks = sort(tokenize($names[$i]));

				# compare this array with the existing array
				my $diff = arrayCmp(\@toks, \@oldToks);

				# our set is equal to one of the previous sets
				if($diff == ARRAYCMP_EQUAL){
					#print "\tCOLLISION (@toks) is equal to (@oldToks)\n";
					# throw it away
					$add = 0;
					last;
				}
				# our set is a subset of one of the previous sets
				elsif($diff == ARRAYCMP_SUBSET){
					#print "\tCOLLISION (@toks) is a subset of (@oldToks)\n";
					# throw it away
					$add = 0;
					last;
				}
				# our set is a superset of one of the previous sets
				elsif($diff == ARRAYCMP_SUPERSET){
					# replace the other set, unless it's the official name!
					if($i > 0){
						#print "\tCOLLISION (@toks) is a superset of (@oldToks)\n";
						splice(@names, $i, 1, $parts[1]);
						$add = 0;
						last;
					}
					else{
						# if it's only a superset of
						# the official name, we'll add
						# it. it'll be cleared later if
						# it conflicts with one of the
						# other alias names.

						#print "\t[COLLISION] (@toks) is a superset of (@oldToks)\n";
						$add = 1;
					}
				}
				# our set contains different elements than the previous set
				else{
					#print "\tALIAS (@toks) is different than (@oldToks)\n";
					$add = 1;
				}
			}

			# if the alias was unique, add it to the list of used
			# names
			if($add){
				push(@names, $parts[1]);
				#print "\tThere are now " . scalar(@names) . " aliases.\n";
			}

		}
		else{
			warn "Warning: Stray alis definition (ID does not match previous official entry).\n";
		}
	}
}

# print out any aliases for the last location
shift(@names);
#print scalar(@names) . " aliases:\n";
foreach(@names){
	print "ALIAS\t$_\n";
}

close(IN);


# compare two arrays: return 1 if they contain the same elements (string
# comparison), or 0 if they are different
sub arrayEq{
	my($a1, $a2) = @_;

	#print "arrayEq: (@$a1) cmp (@$a2)\n";

	# first compare lengths
	if(scalar(@$a1) != scalar(@$a2)){
		return 0;
	}

	# then compare each element
	for my $i (0..$#{$a1}){
		if($a1->[$i] ne $a2->[$i]){
			return 0;
		}
	}
	return 1;
}

# return the appropriate constant if the first array is a subset of, equal to,
# or a superset of, the second array, respectively. if the two sets have
# non-common elements, ARRAYCMP_DIFF is returned.
sub arrayCmp{
	my($a1, $a2) = @_;

	# figure out which array is longer
	my $larger;
	my $smaller;
	if(scalar(@$a1) > scalar(@$a2)){
		$larger = $a1;
		$smaller = $a2;
	}
	else{
		$larger = $a2;
		$smaller = $a1;
	}

	# remember all the values in the larger array
	my %h;
	foreach(@$larger){
		#print "in larger array: $_\n";
		$h{$_} = 1;
	}

	# make sure the smaller array only contains values in the larger one
	my $extra = 0;
	foreach(@$smaller){
		#print "checking if exists in larger array: $_\n";
		if(! exists($h{$_}) ){
			#print "extra value in smaller array: $_\n";
			$extra = 1;
		}
	}

	# if the smaller one contains extras, the arrays have non-common items
	if($extra){
		return ARRAYCMP_DIFF;
	}
	# otherwise, the larger one is a superset of the smaller one
	else{
		# if the sizes are equal, they are equal
		if(scalar(@$larger) == scalar(@$smaller)){
			return ARRAYCMP_EQUAL;
		}
		# otherwise, one is a proper superset of the other
		else{
			# the first arg was the larger set
			if($larger == $a1){
				return ARRAYCMP_SUPERSET;
			}
			# the first arg was the smaller et
			else{
				return ARRAYCMP_SUBSET;
			}
		}
	}
}
