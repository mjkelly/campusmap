#!/usr/bin/perl -T
# vim: tabstop=4 shiftwidth=4
# -----------------------------------------------------------------
# offline-paths.pl -- Generate path images for all pairs of locations, at all
# zoom levels. This is every path image we'll ever need for the given map data.
#
# Copyright 2005 Michael Kelly and David Lindquist
# -----------------------------------------------------------------

use strict;
use warnings;

# the first line is for packages installed on the server.
# the second is for all our own libraries
use lib qw(
	./lib
);

use CGI;
#use FCGI;
use File::Temp ();
use HTML::Template;
use GD;
use POSIX qw(strftime);

# import lots of stuff from lots of different places...
use MapGlobals qw(TRUE FALSE INFINITY between asInt round getWords @SIZES @SCALES plog);
use LoadData qw(nameNormalize findLocation findKeyword isKeyword getKeyText);
use MapGraphics;
use ShortestPath;
use InterfaceLib qw(state listZoomLevels buildHelpText buildKeywordText
	buildLocationOptions buildLocationList pickZoom formatTime);

# we always need all the locations, so load them off disk
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);

# these may or may not be loaded later on
my $points = LoadData::loadPoints($MapGlobals::POINT_FILE);
my %dijkstra = ();

my @keys = keys(%{$locations->{'ByID'}});
@keys = sort {int($a) <=> int($b)} @keys;

my $md_dir = 'paths-metadata';
mkdir($md_dir);

my $total = (@keys*(@keys-1))/2;
my $count = 0;

print "keys = @keys\n\n";

for my $id0 (@keys) {
    for my $id1 (@keys) {
        $id0 = int($id0);
        $id1 = int($id1);
        #print "---> $id0 $id1\n";

        # This is the only non-obvious part: We restrict ourselves to (id0,id1)
        # pairs where id0 < id1. This eliminates duplicate paths (i.e., 1->4 vs
        # 4->1) and paths where the source and destination are the same (i.e.,
        # 1->1, 4->4).
        if ($id0 >= $id1) {
            next;
        }
        $count++;
        print "$count/$total: $id0 $id1\n";

        my ($dist, $rect, $pathPoints, $points) = LoadData::loadShortestPath($locations->{'ByID'}{$id0},
                                                                          $locations->{'ByID'}{$id1},
                                                                          \%dijkstra,
                                                                          $points);
        my $pathImgRect = MapGraphics::makePathImages($id0, $id1, $rect, $pathPoints);
        #print "d = ", length(%dijkstra), "\n";
        open(METADATA, '>', "$md_dir/$id0-$id1");
        for my $k (keys(%$rect)) {
            print METADATA "rect_$k=", $rect->{$k}, "\n";
        }
        for my $k (keys(%$pathImgRect)) {
            print METADATA "path_img_rect_$k=", $pathImgRect->{$k}, "\n";
        }
        $dist = -1 if (!defined($dist));
        print METADATA "dist=$dist\n";
        close(METADATA);
    }
}

