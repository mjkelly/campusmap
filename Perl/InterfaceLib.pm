# -----------------------------------------------------------------
# InterfaceLib.pm -- Functions for the interface. Many generate HTML to feed to
# the template.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Thu Aug 25 20:01:08 PDT 2005
# -----------------------------------------------------------------

package InterfaceLib;

require Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(state listZoomLevels buildHelpText buildKeywordText
	buildLocationOptions buildLocationList pickZoom formatTime);
@EXPORT = qw();

use strict;
use warnings;

use MapGlobals qw(INFINITY asInt round);

###################################################################
# Given information about the current map state, return a query string that
# will preserve the given state.
# Args:
#	- source location, as text
#	- destination location, as text
#	- x-offset
#	- y-offset
#	- map scale
#	- viewing window size
#	- minutes it takes the user to walk one mile
#	- template name ("mode")
# Returns:
#	- query string
###################################################################
sub state{
	my ($from, $to, $x, $y, $scale, $size, $mpm, $mode) = (@_);
	my @keys = qw(from to xoff yoff scale size mpm mode);
	my $str;
	for my $i (0..$#_){
		if(defined($_[$i])){
			$str .= '&amp;' if defined($str);
			$str .= "$keys[$i]=$_[$i]";
		}
	}
	return $str;
}

###################################################################
# Return a list of zoom levels that can be used to build a zoom widget inside
# the template.
# Args:
#	Identical to state().
# Returns:
#	- an array of hashrefs containing info for each zoom level
###################################################################
sub listZoomLevels{
	my ($from, $to, $x, $y, $scale, $size, $mpm, $mode) = (@_);

	my @ret;
	for my $i (0..$#MapGlobals::SCALES){
		push(@ret, {
			URL => $MapGlobals::SELF . '?' . state($from, $to, $x, $y, $i, $size, $mpm, $mode),
			SELECTED => ($i == $scale),
			LEVEL => $i,
		});
	}

	return \@ret;
}

###################################################################
# Given EITHER the 'from' search text or the 'to' search text (there are two
# arguments, but one should be undefined), some script state, and a list of IDs
# of suggested locations, build and return HTML for a list that shows the
# suggested locations.
#
# If given a list of GraphPoints run through ShortestPath::find as well, each
# location's distance from the source location (determined by find()) will also
# be listed.
#
# This is for when a search has failed, and a number of alternate matches have
# been found.
#
# Args:
#	- 'from' search text (or undef)
#	- 'to' search text (or undef)
#	- miles per minute (to perserve state)
#	- template mode (to perserve state)
#	- hashref of locations
#	- hashref of GraphPoints (optional, to list distances)
#	- arrayref of location IDs (ints) that are possible matches to the
#	  search term
###################################################################
sub buildHelpText{
	my ($fromTxt, $toTxt, $mpm, $mode, $locations, $points, $ids) = (@_);

	# figure out which kind of help we're giving
	# one of $fromTxt and $toTxt will be undef. whichever one is,
	# that's the one we have a list of IDs for.
	my $helpfor = defined($fromTxt) ? 'to' : 'from';

	# add distances to the list of IDs
	if( defined($points) ){
		foreach (@$ids){
			my $target = $points->{$locations->{'ByID'}{$_->{'id'}}{'PointID'}};
			$_->{'dist'} = ShortestPath::distTo($points, $target);
		}
	}

	my $str = buildLocationOptions($helpfor, "Did you mean...", $ids, $locations, $fromTxt, $toTxt, $mpm, $mode);
	return $str;
}

###################################################################
# Analogous to buildHelpText, but builds text for keyword searches.
#
# Args (same as buildHelpText()):
#	- 'from' search text (or undef)
#	- 'to' search text (or undef)
#	- miles per minute (to perserve state)
#	- template mode (to perserve state)
#	- hashref of locations
#	- hashref of GraphPoints (optional, to list distances)
#	- arrayref of location IDs (ints) that are possible matches to the
#	  search term
###################################################################
sub buildKeywordText{
	my ($fromTxt, $toTxt, $mpm, $mode, $locations, $points, $ids) = (@_);

	# internally-defined maximum number of locations to list
	my $max = 10;

	# figure out which kind of help we're giving
	# one of $fromTxt and $toTxt will be undef. whichever one is,
	# that's the one we have a list of IDs for.
	my $helpfor = defined($fromTxt) ? 'to' : 'from';

	# add distances to the list of IDs
	if( defined($points) ){
		foreach (@$ids){
			my $target = $points->{$locations->{'ByID'}{$_->{'id'}}{'PointID'}};
			$_->{'dist'} = ShortestPath::distTo($points, $target);
		}

		# now we sort by those distances
		@$ids = sort { $a->{'dist'} <=> $b->{'dist'} } @$ids;

		if(@$ids > $max){
			@$ids = @$ids[0..$max-1];
		}
	}

	my $str = buildLocationOptions($helpfor, '', $ids, $locations, $fromTxt, $toTxt, $mpm, $mode);
	return $str;
}

###################################################################
# Build a list of locations, with links that allow completion of the current
# search with each location listed. Used to list results of fuzzy matching or
# keyword searches.
#
# Args (same as buildHelpText()):
#	- a string, 'from' or 'to', indicating whether the text we're building
#	  contains suggestions for the 'from' or 'to' location, respectively.
#	- a title for the list of suggestions
#	- an arrayref of matching IDs, as gotten from findLocation() or
#	  findKeyword(). If distances to each location should be displayed,
#	  each item of this array (a hashref) should contain a key 'dist' that
#	  lists the distance in raw base pixels.
#	- hashref of locations
#	- 'from' search text
#	- 'to' search text
#	- miles per minute (to perserve state)
#	- template mode (to perserve state)
###################################################################
sub buildLocationOptions{
	my($helpfor, $title, $ids, $locations, $fromTxt, $toTxt, $mpm, $mode) = @_;
	my $str = (length($title) ? "<p><b>$title</b></p>\n" : '') . "<ol>\n";
	my $url;
	foreach (@$ids){
		# build distance text
		my $dist_txt = '';
		if( exists($_->{'dist'}) ){
			if($_->{'dist'} == INFINITY){
				$dist_txt = ' (&#8734;)'; # this is the infinity symbol
			}
			elsif($_->{'dist'} == 0){
				$dist_txt = '';
			}
			else{
				$dist_txt = sprintf(" (%0.2f mi)",
					$_->{'dist'}/$MapGlobals::PIXELS_PER_UNIT);
			}
		}

		# we need to avoid flipping the 'from' and 'to' locations here, so
		# we're careful about which location we're actually searching for
		if($helpfor eq 'from'){
			$url = state(CGI::escape($locations->{'ByID'}{$_->{'id'}}{'Name'}), $toTxt,
				undef, undef, undef, undef, $mpm, $mode);
		}
		else{
			$url = state($fromTxt, CGI::escape($locations->{'ByID'}{$_->{'id'}}{'Name'}),
				undef, undef, undef, undef, $mpm, $mode);
		}
		$str .= sprintf(qq|\t<li><a href="$MapGlobals::SELF?%s">%s</a>%s</li>\n|,
			$url, CGI::escapeHTML($locations->{'ByID'}{$_->{'id'}}{'Name'}), $dist_txt);
	}
	$str .= "</ol>\n";

	return $str;
}

###################################################################
# Builds two strings containing the <option> tags that list source and
# destination locations. This is a very template-ish thing to do, but we do it
# here for speed reasons.
#
# Args:
#	- location hashref
###################################################################
sub buildLocationList{
	my($locations) = @_;
	my $txt = '';
	my($trunc, $name);

	foreach (sort keys %{$locations->{'ByName'}}){
		$trunc = $name = $locations->{'ByName'}{$_}{'Name'};

		# truncate the name if it's too long
		if(length($name) > $MapGlobals::MAX_NAME_LEN){
			$trunc = substr($name, 0, $MapGlobals::MAX_NAME_LEN) . '...';
		}

		$txt .= sprintf(qq{<option value="%s">%s</option>\n},
			CGI::escapeHTML($name),
			CGI::escapeHTML($trunc)
		);

	}

	return $txt;
}

###################################################################
# Given information about the path that's being drawn, pick a zoom level (and,
# possibly, a set of x/y offsets) that will display the two locations as well
# as possible.
# Args:
# 	- source location ref
#	- destination location ref
#	- current x offset
#	- current y offset
#	- viewport width
#	- viewport height
#	- a hashref containing the bounding rectangle for the path, such as
#	  returned by LoadData::loadCache or ShortestPath::pathPoints
#	- an arrayref of the available zoom levels. This should probably be
#	  \@MapGlobals::SCALES.
# Returns:
#	- an index into the provided zoom level arrayref, specifying the
#	  correct zoom level.
#	- a new x offset
#	- a new y offset
###################################################################
sub pickZoom{
	my($fromLoc, $toLoc, $xoff, $yoff, $width, $height, $rect, $scales) = @_;
	my $scale;

	# if x/y offsets aren't set, use the center of the bounding rectangle
	if( !$xoff && !$yoff ){
		$xoff = int(($rect->{'xmin'} + $rect->{'xmax'}) / 2);
		$yoff = int(($rect->{'ymin'} + $rect->{'ymax'}) / 2);
	}

	my $w = $rect->{'xmax'} - $rect->{'xmin'};
	my $h = $rect->{'ymax'} - $rect->{'ymin'};

	# find the first level that encompasses the entire rectangle
	for my $i (0..$#{$scales}){
		if($w < $width/$scales->[$i] && $h < $height/$scales->[$i]){
			# we know it's big enough. now, make sure
			# nothing's too close to the edges

			# x pixels on the actual output image
			# correspond to x/$scales->[$i] pixels on the
			# base image (to counteract the scale
			# multiplier).

			# if any of the locations are too close to any edge,
			# reject this zoom level and move to the next one
			if(abs($xoff - $fromLoc->{'x'}) > $width/(2*$scales->[$i]) - 5){
				next;
			}
			if(abs($xoff - $toLoc->{'x'}) > $width/(2*$scales->[$i]) - 5){
				next;
			}

			if(abs($yoff - $fromLoc->{'y'}) > $height/(2*$scales->[$i]) - 5){
				next;
			}
			if(abs($yoff - $toLoc->{'y'}) > $height/(2*$scales->[$i]) - 5){
				next;
			}

			# if location names would ordinarily go off the
			# edge of the screen, drawLocation draws them
			# to the left instead

			# if we made it this far, this is a good scale!
			$scale = $i;
			last;
		}
	}

	# if $scale is still a bogus value, we couldn't find ANY zoom level
	# to accomodate the two locations! fall back to centering on the destination,
	# and zooming in a bit
	if(!defined($scale)){
		$scale = $MapGlobals::SINGLE_LOC_SCALE;
		$xoff = $toLoc->{'x'};
		$yoff = $toLoc->{'y'};
	}

	return ($scale, $xoff, $yoff);
}


###################################################################
# Given a number of minutes, format it nicely.
# Args:
#	- number of minutes
# Returns:
#	- a nice string representing the argument
###################################################################
sub formatTime{
	my $t = shift;
	my $min = asInt($t);
	my $low = $t - $min;
	my $secs = round($low*60);
	$secs = '0' . $secs if($secs < 10);
	return round($min) . ':' . $secs;
}

1;
