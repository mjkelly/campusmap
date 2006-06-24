#!/usr/bin/perl
# -----------------------------------------------------------------
# keywords.pl -- List locations with a given keyword.
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Sun Sep 11 23:26:09 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use lib qw(./lib);

use MapGlobals qw(INFINITY);
use LoadData;
use ShortestPath;

sub usage{
	print <<_USAGE_;
usage: $0 KEYWORD

Lists all locations that contain the keyword KEYWORD.
_USAGE_
	exit(1);
}

# load data
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);
#my $points = LoadData::loadPoints($MapGlobals::POINT_FILE);

# the input string can be anything we'd type into the interface
my $keyword = shift(@ARGV) or usage();

# find the right location
#my @ids = LoadData::findLocation($name, $locations);

if( exists($locations->{'ByKeyword'}{$keyword}) ){
	print "Locations with keyword: $keyword...\n";
	foreach (@{$locations->{'ByKeyword'}{$keyword}}){
		print "\t$_->{'Name'} ($_->{'ID'})\n";
	}


	my $n = scalar @{$locations->{'ByKeyword'}{$keyword}};
	print "*** $n locations with keyword: $keyword.\n";
}
else{
	print "No locations exist with keyword: $keyword.\n";
}

