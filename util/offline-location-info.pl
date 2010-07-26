#!/usr/bin/perl -T
# vim: tabstop=4 shiftwidth=4
# -----------------------------------------------------------------
# offline-locations.pl -- Export location info in a readable format. This is
# questionable useful since we already have the XML file.
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

use Carp ();
local $SIG{__WARN__} = \&Carp::cluck;
local $SIG{__DIE__} = \&Carp::croak;


# we always need all the locations, so load them off disk
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);

my @ids = keys(%{$locations->{'ByID'}});
@ids = sort {int($a) <=> int($b)} @ids;

# BuildingCode = wlh
# ID = 468
# x = 5405
# Aliases = ARRAY(0x2642150)
# Keywords =
# y = 3276
# DisplayName = 1
# PointID = 1972
# Name = Warren Lecture Hall

for my $id (@ids) {
    my $loc = $locations->{'ByID'}{$id};
    for my $k qw(Name ID BuildingCode x y Keywords) {
        print "$k=", $loc->{$k}, "\n";
    }
    for my $elem (@{$loc->{'Aliases'}}) {
        print "Aliases=$elem\n";
    }
    print "\n";
}


# build a list of printable location names
#my $loc_opt = buildLocationList($locations);

#print $loc_opt;
