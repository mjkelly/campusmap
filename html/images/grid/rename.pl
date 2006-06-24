#!/usr/bin/perl
# -----------------------------------------------------------------
# rename.pl -- Rename grid images to lower zoom IDs.
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Thu Sep  8 23:17:11 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

my %files;
opendir(DIR, '.') or die "$!";
while($_ = readdir(DIR)){
	if(/visitor-(\d)\[\d+\]\[\d+\]\.png/){
		my $n = $1;
		#print "files{$n} += $_\n";
		push(@{$files{$n}}, $_);
	}
}
closedir(DIR);

foreach my $n (sort keys %files){
	foreach(@{$files{$n}}){
		if($n == 0){
			print "unlink($_)\n";
			unlink($_);
		}
		else{
			my $n1 = $n - 1;
			if(/visitor-\d\[(\d+)\]\[(\d+)\]\.png/){
				my($x, $y) = ($1, $2);
				my $newname = "visitor-$n1\[$x\]\[$y\].png";
				print "rename($_, $newname)\n";
				rename($_, $newname);
			}
		}
	}
}
