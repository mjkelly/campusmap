#!/usr/bin/perl
# -----------------------------------------------------------------
# disconnected.pl -- List any locations that are entirely disconnected from the
# given location. Good for data testing.
#
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 2 of the License,
# or (at your option) any later version.
#
# Mon Sep  5 14:08:32 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use lib qw(./lib);

use MapGlobals qw(INFINITY);
use LoadData;
use ShortestPath;

sub usage{
	print <<_USAGE_;
usage: $0 LOCNAME

Lists all locations disconnected from location LOCNAME.
_USAGE_
	exit(1);
}

# load data
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);
my $points = LoadData::loadPoints($MapGlobals::POINT_FILE);

# the input string can be anything we'd type into the interface
my $name = shift(@ARGV) or usage();

# find the right location
my @ids = LoadData::findLocation($name, $locations);

# make sure we got one and only one match
if(@ids > 1){
	print "More than one location matches '$name'. Give me something more specific.\n";
	print "Matching locations:\n";
	foreach (@ids){
		print "\t$_->{'text'} (ID $_->{'id'})\n";
	}
	exit(1);
}
elsif(@ids == 0){
	print "Location '$name' not found!\n";
}

my $id = $ids[0]{'id'};
my $loc_name = $locations->{'ByID'}{$id}{'Name'};

print "Listing locations connected to location ID $id ($loc_name)...\n";

# run Dijkstra's with the GraphPoint of the given location as the center
ShortestPath::find($locations->{'ByID'}{$id}{'PointID'}, $points);

# check the distances of every location-point from the given one
my($pid, $dist);
my @disco;
foreach(keys %{$locations->{'ByID'}}){
	$pid = $locations->{'ByID'}{$_}{'PointID'};
	$dist = $points->{$pid}{'Distance'};
	if($dist == INFINITY){
		push(@disco, $_);
	}
}

my $count = scalar @disco;
foreach (sort {$locations->{'ByID'}{$a}{'Name'} cmp $locations->{'ByID'}{$b}{'Name'}} @disco){
	print "\t$locations->{'ByID'}{$_}{'Name'} ($_)\n";
}

# print some stats
print "\n";
my $tot = scalar(keys %{$locations->{'ByID'}});
my $frac = ($tot != 0) ? (($tot-$count)/$tot) : 0;
printf("*** %d of %d (%.1f%%) locations are connected to %d (%s).\n",
	($tot-$count), $tot, $frac*100.0, $id, $loc_name);

if($tot == $count + 1){
	print "*** It looks like $loc_name is disconnected from the rest of the map.\n";
}
