#!/usr/bin/perl -T
# vim: tabstop=4 shiftwidth=4
# -----------------------------------------------------------------
# map.cgi -- The user interface for the UCSDMap.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# TODO:
#	- Create some kind of 'viewport' object to avoid passing around
# 	  these huge, indecipherable (and frequently-changing!) lists.
#
# $Id$
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

my %pairs = ();

my @keys = keys(%{$locations->{'ByID'}});

for my $id0 (@keys) {
    for my $id1 (@keys) {
        $id0 = int($id0);
        $id1 = int($id1);
        if ($id0 > $id1) {
            my $tmp = $id1;
            $id1 = $id0;
            $id0 = $tmp;
            print "swapping $id0 $id1\n";
        }
        elsif ($id0 == $id1) {
            next;
        }
        if (defined($pairs{"$id0-$id1"})) {
            print "Already did $id0 $id1\n";
            next;
        }

        $pairs{"$id0-$id1"} = 1;
        print "$id0 $id1\n";

        my ($dist, $rect, $pathPoints, $points) = LoadData::loadShortestPath($locations->{'ByID'}{$id0},
                                                                          $locations->{'ByID'}{$id1},
                                                                          \%dijkstra,
                                                                          $points);
        my $pathImgRect = MapGraphics::makePathImages($id0, $id1, $rect, $pathPoints);
        print "d = ", length(%dijkstra), "\n";
    }
}

exit(1);

# -----------------------------------------------------------------
# Do startup stuff: load data, convert some of the input data, initialize
# variables, etc.
# -----------------------------------------------------------------

# HTML-safe versions of the from and to text
# my $fromTxtSafe = CGI::escapeHTML($fromTxt);
# my $toTxtSafe = CGI::escapeHTML($toTxt);
#
# # width and height of the viewing window (total image size is in MapGlobals)
# my $width  = $SIZES[$size][0];
# my $height = $SIZES[$size][1];
#
# my $ERROR = '';
#
# # -----------------------------------------------------------------
# # Now that we have source and destination IDs, build up some more variables
# # based on that.
# # -----------------------------------------------------------------
#
# # build a list of printable location names
# my $loc_opt = buildLocationList($locations);
#
# # we actually run the shortest-path algorithm only if we found both
# # the source and destination locations
# my $havePath = ($src_found && $dst_found);
#
# # URL-safe versions of the from and to text
# my $fromTxtURL = CGI::escape($fromTxt);
# my $toTxtURL = CGI::escape($toTxt);
#
# # these store the _path_ IDs of the starting and ending point, which we
# # need to actually find the shortest path
# my $startID = 0;
# my $endID = 0;
#
# # if start and end locations are the same, don't draw
# if( $from == $to ){
#         $havePath = FALSE;
# }
# # otherwise, assign the start and end IDs to meaningful values
# else{
#         $startID = $locations->{'ByID'}{$from}{'PointID'};
#         $endID = $locations->{'ByID'}{$to}{'PointID'};
# }
#
# # -----------------------------------------------------------------
# # Figure out the shortest path between the two locations, if applicable. This
# # is also where we handle the various cases such as when only one location is
# # found but the other has multiple fuzzy possibilities, etc.
# # -----------------------------------------------------------------
#
# # do the shortest path stuff
# my $dist = 0;
# my $rect;
# my $pathPoints;
# if($havePath){
#
#         # get the path between the two locations
#         ($dist, $rect, $pathPoints, $points) = LoadData::loadShortestPath(
#                 $locations->{'ByID'}{$from}, $locations->{'ByID'}{$to}, \%dijkstra, $points);
#
#         # adjust the pixel distance to the unit we're using to display it (mi, ft, etc)
#         if(defined($dist)){
#                 $dist /= $MapGlobals::PIXELS_PER_UNIT;
#         }
#         else{
#                 # if $dist is undefined, there's no path between the two
#                 # locations.
#                 $havePath = FALSE;
#                 $ERROR .= "These two locations are not connected. ";
#         }
#
#         # now pick a zoom level, if one hasn't already been specified
#         if( !defined($scale) ){
#                 ($scale, $xoff, $yoff) = pickZoom(
#                         $locations->{'ByID'}{$from}, $locations->{'ByID'}{$to},
#                         $xoff, $yoff, $width, $height, $rect, \@SCALES);
#         }
# }
#
#
# # if we don't have a full path, check if we have a single location,
# # and center on that
# else{
#         # error if we got no matches
#         if(@fromids == 0){
#                 if($src_keyword){
#                         $ERROR .= "<p><b>&quot;" . getKeyText($fromTxtSafe) . "&quot; is not a valid keyword.</b></p>\n";
#                 }
#         }
#         # if we got multiple matches, build help text to disambiguate
#         elsif(@fromids > 1){
#                 # if we found a good 'from' location, run shortest path stuff
#                 # so we can display distances
#                 if($dst_found){
#                         $points = LoadData::loadPoints($MapGlobals::POINT_FILE) unless defined($points);
#                         # get the result of Dijkstra's somehow
#                         if( !defined( $dijkstra{$endID})
#                                         && !defined($dijkstra{$endID} = LoadData::readDijkstraCache($endID)) ){
#                                 $dijkstra{$endID} = ShortestPath::find($endID, $points);
#                                 LoadData::writeDijkstraCache($dijkstra{$endID}, $endID);
#                         }
#                 }
#                 if($src_keyword){
#                         $src_help = "<p><b>Closest matches for $fromTxtSafe...</b></p>"
#                                 . buildKeywordText(undef, $toTxtURL, $mpm, $template, $locations, $points, $dijkstra{$endID}, \@fromids);
#                 }
#                 else{
#                         $src_help = "<p><b>Start location &quot;$fromTxtSafe&quot; not found.</b></p>"
#                                 . buildHelpText(undef, $toTxtURL, $mpm, $template, $locations, $points, $dijkstra{$endID}, \@fromids);
#                 }
#         }
#
#         if(@toids == 0){
#                 if($dst_keyword){
#                         $ERROR .= "<p><b>&quot;" . getKeyText($toTxtSafe) . "&quot; is not a valid keyword.</b></p>\n";
#                 }
#         }
#         elsif(@toids > 1){
#                 # if we found a good 'to' location, run shortest path stuff
#                 # so we can display distances
#                 if($src_found){
#                         $points = LoadData::loadPoints($MapGlobals::POINT_FILE) unless defined($points);
#                         # get the result of Dijkstra's somehow
#                         if( !defined( $dijkstra{$startID})
#                                         && !defined($dijkstra{$startID} = LoadData::readDijkstraCache($startID)) ){
#                                 $dijkstra{$startID} = ShortestPath::find($startID, $points);
#                                 LoadData::writeDijkstraCache($dijkstra{$startID}, $startID);
#                         }
#                 }
#                 if($dst_keyword){
#                         $dst_help = "<p><b>Closest matches for $toTxtSafe...</b></p>"
#                                 . buildKeywordText($fromTxtURL, undef, $mpm, $template, $locations, $points, $dijkstra{$startID}, \@toids);
#                 }
#                 else{
#                         $dst_help = "<p><b>Destination location &quot;$toTxtSafe&quot; not found.</b></p>"
#                                 . buildHelpText($fromTxtURL, undef, $mpm, $template, $locations, $points, $dijkstra{$startID}, \@toids);
#                 }
#         }
#
#         if(!$xoff && !$yoff){
#                 if($src_found){
#                         $xoff = $locations->{'ByID'}{$from}{'x'};
#                         $yoff = $locations->{'ByID'}{$from}{'y'};
#                 }
#                 elsif($dst_found){
#                         $xoff = $locations->{'ByID'}{$to}{'x'};
#                         $yoff = $locations->{'ByID'}{$to}{'y'};
#                 }
#         }
#
#         # use the default scale
#         if(!defined($scale)){
#                 # we use different levels of zoom depending on whether we're
#                 # not focused on _anything_, or if the user selected a single location
#                 if($src_found || $dst_found){
#                         $scale = $MapGlobals::SINGLE_LOC_SCALE;
#                 }
#                 else{
#                         $scale = $MapGlobals::DEFAULT_SCALE;
#                 }
#         }
# }
#
# # if we still don't have offsets, use the default ones
# if(!$xoff && !$yoff){
#         $xoff = $MapGlobals::DEFAULT_XOFF;
#         $yoff = $MapGlobals::DEFAULT_YOFF;
# }
#
# # -----------------------------------------------------------------
# # Create the path images (if we're in javascript view).
# # -----------------------------------------------------------------
# my($pathImgRect);
# # if we're using the javascript template, write scaled path images
# if ($template eq 'js'){
#         if($havePath){
#                 # create the path images for this path at all scales
#                 $pathImgRect = MapGraphics::makePathImages($from, $to, $rect, $pathPoints);
#         }
# }
#
#
