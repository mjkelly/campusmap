#!/usr/bin/perl
# -----------------------------------------------------------------
# rename2.pl -- Rename grid images from style:
#   visitor-0[12][34].png
# to style:
#   0-12-34.png
#
# Shorter and avoids url-encoding.
#
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
		if(/visitor-\d\[(\d+)\]\[(\d+)\]\.png/){
			my($x, $y) = ($1, $2);
			my $newname = "$n-$x-$y.png";
			print "rename($_, $newname)\n";
			rename($_, $newname);
		}
	}
}
