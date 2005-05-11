#!/usr/bin/perl
# -----------------------------------------------------------------
# map.cgi -- The user interface for the UCSDMap.
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Sun May  1 17:37:51 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use CGI;
use MapGlobals
	qw(@SCALES INFINITY TRUE FALSE $DYNAMIC_IMG_DIR $STATIC_IMG_DIR);
use LoadData;
use MapGraphics;
use ShortestPath;
use File::Temp ();

my $q = CGI::new();

# window sizes
my @sizes = (
	[400, 300],
	[500, 375],
	[640, 480],
);

# the name of this script, for form actions, etc
our $self = 'map.cgi';

# how far (in pixels) to pan when the user clicks "left", "right", "up", or
# "down"
my $pan = 100;

# generate a dynamic filename
# (this can be used as a string to get its filename, toString()-style)
my $tmpfile = new File::Temp(
		TEMPLATE => 'ucsdmap-XXXXXX',
		DIR => $DYNAMIC_IMG_DIR,
		SUFFIX => $MapGlobals::DYNAMIC_IMG_SUFFIX,
		UNLINK => 0,
	);
my $fname = $tmpfile->filename;
chmod(0644, $fname);

# load all the data we'll need
my $points	= LoadData::loadPoints($MapGlobals::POINT_FILE);
my $locations	= LoadData::loadLocations($MapGlobals::LOCATION_FILE);
my $edges	= LoadData::loadEdges($MapGlobals::EDGE_FILE);

# source and destination location names
my $fromTxt = $q->param('from') || '';
my $toTxt   = $q->param('to')   || '';

# HTML-safe versions of the from and to text
my $fromTxtSafe = CGI::escapeHTML($fromTxt);
my $toTxtSafe = CGI::escapeHTML($toTxt);

# normalized versions of the from and to text
my $fromTxtLookup = LoadData::nameLookup($fromTxt);
my $toTxtLookup = LoadData::nameLookup($toTxt);

# x and y display offsets, from the upper left
my $xoff   = int($q->param('xoff')   || 0);
my $yoff   = int($q->param('yoff')   || 0);
my $scale  =     $q->param('scale')  || 0;
my $size   = int(defined($q->param('size')) ? $q->param('size') : 1);
#my $size   = int($q->param('size'));

# width and height of the viewing window (total image size is in MapGlobals)
my $width  = $sizes[$size][0];
my $height = $sizes[$size][1];

# click offsets from the map
# (these are the only two input variables that could be undefined)
my $mapx = defined($q->param('map.x')) ? int($q->param('map.x')) : undef;
my $mapy = defined($q->param('map.y')) ? int($q->param('map.y')) : undef;

# clear out any old images
MapGlobals::reaper($MapGlobals::DYNAMIC_MAX_AGE, $MapGlobals::DYNAMIC_IMG_SUFFIX);

# whether source and destination locations were found
my ($src_found, $dst_found) = (TRUE, TRUE);

# now convert $fromTxt and $toTxt to IDs
my $ERROR = '';
my ($from, $to) = (0, 0);

# if we got no input, put up an informative message
if($fromTxt eq '' && $toTxt eq ''){
	$ERROR .= "Enter a start and destination location to find the shortest"
		. " path between the two, or select only one to zoom to that location.";

}
# otherwise, attempt to look up both locations
else{
	# the ordering of these two if-blocks is significant!
	# switching them results in the view centering on the START location.

	if( exists($locations->{$toTxtLookup}) ){
		$to = $locations->{$toTxtLookup}{'ID'};

		# set the offsets to the coords of the 'to' location
		if(!$xoff && !$yoff){
			$xoff = $locations->{$to}{'x'};
			$yoff = $locations->{$to}{'y'};
		}
	}
	elsif($toTxt ne ''){
		# fall back to fuzzy matching here
		my $id = LoadData::findName($toTxt, $locations);
		if($id != -1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$to = $id;
			$toTxt = $locations->{$id}{'Name'};
			$toTxtSafe = CGI::escapeHTML($toTxt);
			$toTxtLookup = LoadData::nameLookup($toTxt);
			if(!$xoff && !$yoff){
				$xoff = $locations->{$to}{'x'};
				$yoff = $locations->{$to}{'y'};
			}
		}
		else{
			# I've done all I can. He's dead, Jim.
			$ERROR .= "<b>Destination location &quot;$toTxtSafe&quot; not found.</b>\n"
				if($toTxt ne '');
			$dst_found = FALSE;
		}
	}
	else{
		$dst_found = FALSE;
	}

	if( exists($locations->{$fromTxtLookup}) ){
		$from = $locations->{$fromTxtLookup}{'ID'};

		# this is only here so that, if the 'to' location doesn't exist,
		# we set the offsets to _something_ reasonable
		if(!$xoff && !$yoff){
			$xoff = $locations->{$from}{'x'};
			$yoff = $locations->{$from}{'y'};
		}
	}
	elsif($fromTxt ne ''){
		# fall back to fuzzy matching here
		my $id = LoadData::findName($fromTxt, $locations);
		if($id != -1){
			# since we found a real location, we've got to reset all the text
			# associated with the location
			$from = $id;
			$fromTxt = $locations->{$id}{'Name'};
			$fromTxtSafe = CGI::escapeHTML($fromTxt);
			$fromTxtLookup = LoadData::nameLookup($fromTxt);
			if(!$xoff && !$yoff){
				$xoff = $locations->{$from}{'x'};
				$yoff = $locations->{$from}{'y'};
			}
		}
		else{
			$ERROR .= "<b>Start location &quot;$fromTxtSafe&quot; not found.</b>\n"
				if($fromTxt ne '');
			$src_found = FALSE;
		}
	}
	else{
		$src_found = FALSE;
	}

}

# URL-safe versions of the from and to text
my $fromTxtURL = CGI::escape($fromTxt);
my $toTxtURL = CGI::escape($toTxt);

# build a list of printable location names
my @locNames = ();
foreach (sort keys %$locations){
	# add each unique name
	if( substr($_, 0, 5) eq 'name:' ){
		push(@locNames, $locations->{$_}{'Name'});
	}
}

# add a 'None' location to the beginning of the list
unshift(@locNames, 'None');
# build menus to change the values of the form fields
# (These menus have no names, so they shouldn't submit any form data)
my $fromMenu = buildMenu(\@locNames, 'main.from.value', $fromTxt);
my $toMenu = buildMenu(\@locNames, 'main.to.value', $toTxt);


# if we still don't have offsets, use the default ones
if(!$xoff && !$yoff){
	$xoff = $MapGlobals::DEFAULT_XOFF;
	$yoff = $MapGlobals::DEFAULT_YOFF;
}

# we actually run the shortest-path algorithm only if we found both
# the source and destination locations
my $path = ($src_found && $dst_found);

# build the status message
my $STATUS = 'No path selected.';
if($fromTxt ne '' || $toTxt ne ''){
	if($path && $fromTxt ne $toTxt){
		$STATUS = "Path from $fromTxtSafe to $toTxtSafe.";
	}
	else{
		if($src_found){
			$STATUS = "Zoomed to $fromTxtSafe.";
		}
		elsif($dst_found){
			$STATUS = "Zoomed to $toTxtSafe.";
		}
	}
}

# integrate the image click offsets (if the image was clicked to recenter)
if( defined($mapx) && defined($mapy) ){
	# adjust the map offsets so they work from the center of the screen
	$mapx -= $width/2;
	$mapy -= $height/2;

	# now adjust our absolute x/y offsets to take into account the last click location
	$xoff += $mapx/$SCALES[$scale];
	$yoff += $mapy/$SCALES[$scale];
}

# get the HTML for the widget that controls map zoom levels (scale)
my $zoomWidget = zoomWidget($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, $size);

# make sure the x/y offsets are in range, and correct them if they aren't
# (but only AFTER we remember them for the links dependent on the current, unadjusted state)
$xoff = between(($width/$SCALES[$scale])/2, $MapGlobals::IMAGE_X - ($width/$SCALES[$scale])/2, $xoff);
$yoff = between(($height/$SCALES[$scale])/2, $MapGlobals::IMAGE_Y - ($height/$SCALES[$scale])/2, $yoff);

# states for the directions
my $left  = state($fromTxtURL, $toTxtURL, $xoff - $pan/$SCALES[$scale], $yoff, $scale, $size);
my $right = state($fromTxtURL, $toTxtURL, $xoff + $pan/$SCALES[$scale], $yoff, $scale, $size);
my $up    = state($fromTxtURL, $toTxtURL, $xoff, $yoff - $pan/$SCALES[$scale], $scale, $size);
my $down  = state($fromTxtURL, $toTxtURL, $xoff, $yoff + $pan/$SCALES[$scale], $scale, $size);

# the diagonal buttons actually cheat: the total movement is about 141 pixels,
# because we move 100 up AND 100 left, for instance. I don't think anyone cares.
# XXX: unused at the moment -- they're generated, but commented-out in the HTML ;)
my $upLeft    = state($fromTxtURL, $toTxtURL,
	$xoff - $pan/$SCALES[$scale], $yoff - $pan/$SCALES[$scale], $scale, $size);
my $upRight   = state($fromTxtURL, $toTxtURL,
	$xoff + $pan/$SCALES[$scale], $yoff - $pan/$SCALES[$scale], $scale, $size);
my $downLeft  = state($fromTxtURL, $toTxtURL,
	$xoff - $pan/$SCALES[$scale], $yoff + $pan/$SCALES[$scale], $scale, $size);
my $downRight = state($fromTxtURL, $toTxtURL,
	$xoff + $pan/$SCALES[$scale], $yoff + $pan/$SCALES[$scale], $scale, $size);

# buttons to make the window bigger/smaller 
my $bigger = state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size < $#sizes) ? $size+1 : $size);
my $smaller = state($fromTxtURL, $toTxtURL, $xoff, $yoff, $scale, ($size > 0) ? $size-1 : $size);

# these store the _path_ IDs of the starting and ending point, which we
# need to actually find the shortest path
my $startID = 0;
my $endID = 0;

# if start and end locations are the same, don't draw
if( $from == $to ){
	$path = FALSE;
}
# otherwise, assign the start and end IDs to meaningful values
else{
	$startID = $locations->{$fromTxtLookup}{'PointID'};
	$endID = $locations->{$toTxtLookup}{'PointID'};
}

# adjust xoff/yoff so they point to the upper-left-hand corner, which is what
# the low-level MapGraphics functions use. additionally, take scale into
# account at the right time, so it doesn't offset scaled views.
my $rawxoff = $xoff*$SCALES[$scale] - $width/2;
my $rawyoff = $yoff*$SCALES[$scale] - $height/2;

# make sure the offsets don't go out of range
$rawxoff = between(0, $MapGlobals::IMAGE_X - $width, $rawxoff);
$rawyoff = between(0, $MapGlobals::IMAGE_Y - $height, $rawyoff);

my $im = GD::Image->newFromGd2Part(MapGlobals::getGd2Filename($SCALES[$scale]),
	$rawxoff, $rawyoff, $width, $height)
	|| die "Could not load image $MapGlobals::BASE_GD2_IMAGE\n";

my $red = $im->colorAllocate(255, 0, 0);
my $green = $im->colorAllocate(0, 255, 0);

# uncomment this to draw ALL paths. useful for debugging.
#MapGraphics::drawAllEdges($edges, $im, 1, $red, $rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);

# do the shortest path stuff
if($path){
	ShortestPath::find($startID, $points);
	my $dist = ShortestPath::drawTo($points, $edges, $points->{$endID}, $im, $green,
		$rawxoff, $rawyoff, $width, $height, $SCALES[$scale]);
	$dist /= $MapGlobals::PIXELS_PER_UNIT;
	$STATUS .= sprintf(" Distance is %.2f %s.", $dist, $MapGlobals::UNITS);
}

MapGraphics::drawAllLocations($locations, $im, $red, $red, $rawxoff, $rawyoff,
				$width, $height, $SCALES[$scale]);

# print the data out to a temporary file
binmode($tmpfile);
print $tmpfile $im->png();
close($tmpfile);

# the tag gets updated whenever this file is commited
my $version = '$Id$';

# now put everthing into a simple output template
# TODO: separate this from the code, maybe?
print <<_HTML_;
Content-type: text/html

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/2000/REC-xhtml1-20000126/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<title>UCSD Map</title>
<meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
</head>
<body bgcolor="#ffffff">

<table border="0" cellpadding="0" cellspacing="0">
<tr>
	<!-- top row of buttons: up-left, up, up-right -->
	<td align="center">
		<!--
		<a href="$self?$upLeft"><img src="$STATIC_IMG_DIR/up-left.png" width="50" height="50" border="0"></a><br />
		-->
	</td>
	<td align="center">
		<a href="$self?$up"><img src="$STATIC_IMG_DIR/up.png" width="128" height="20" border="0"></a><br />
	</td>
	<td align="center">
		<!--
		<a href="$self?$upRight"><img src="$STATIC_IMG_DIR/up-right.png" width="50" height="50" border="0"></a><br />
		-->
	</td>
</tr>

<tr>
	<!-- left button -->
	<td valign="center">
		<a href="$self?$left"><img src="$STATIC_IMG_DIR/left.png" width="20" height="128" border="0"></a>
	</td>

	<!-- the map itself -->
	<td valign="center">

		<table border="1"><tr><td>
			<form method="get" action="$self" target="_self">

			<!-- for preserving state when the user clicks the map -->
			<input type="hidden" name="xoff" value="$xoff" />
			<input type="hidden" name="yoff" value="$yoff" />
			<input type="hidden" name="scale" value="$scale" />
			<input type="hidden" name="size" value="$size" />

			<input id="from" type="hidden" name="from" value="$fromTxtSafe" />
			<input id="to" type="hidden" name="to" value="$toTxtSafe" />

			<!-- the image itself is really a form input, to allow center-on-click -->
			<input type="image" name="map" width="$width" height="$height" border="0"
				src="$fname" />
			</form>
		</td></tr></table>

	</td>

	<!-- right button -->
	<td valign="center">
		<a href="$self?$right"><img src="$STATIC_IMG_DIR/right.png" width="20" height="128" border="0"></a>
	</td>

	<td rowspan="3" align="center" valign="top">
		<!-- begin control panel -->
		<table border="1" bgcolor="#cccccc" cellpadding="2">
		<tr><td>
			$zoomWidget
			<br />
			Window Size: $width x $height
			<table border="0"><tr>
				<td><small>[<a href="$self?$smaller">Smaller</a>]</small></td>
				<td><small>[<a href="$self?$bigger">Bigger</a>]</small></td>
			</tr></table>

			<form method="get" action="$self" target="_self" name="main">
			<!-- remember zoom level and window size when searching --> 
			<input type="hidden" name="scale" value="$scale" />
			<input type="hidden" name="size" value="$size" />
			<table border="0" cellpadding="2" cellspacing="2">
				<tr>
					<td><label for="from">From:</label></td>
					<td>
						<input id="from" type="text" name="from" value="$fromTxtSafe" />
						$fromMenu
					</td>
				</tr>
				<tr>
					<td><label for="to">To:</label></td>
					<td>
						<input id="to" type="text" name="to" value="$toTxtSafe" />
						$toMenu
					</td>
				</tr>
				<tr>
					<td><input type="submit" name="submit" value="Submit" /></td>
				</tr>
			</table>

			</form>
		</td></tr>
		<tr><td>
			<!-- status messages inserted here -->
			$STATUS
		</td></tr>
		</table>
		<!-- end control panel -->

		<p>$ERROR</p>

	</td>
</tr>

<!-- bottom row of buttons: down-left, down, down-right -->
<tr>
	<td align="center">
		<!--
		<a href="$self?$downLeft"><img src="$STATIC_IMG_DIR/down-left.png" width="50" height="50" border="0"></a>
		-->
	</td>
	<td align="center">
		<a href="$self?$down"><img src="$STATIC_IMG_DIR/down.png" width="128" height="20" border="0"></a>
	</td>
	<td align="center">
		<!--
		<a href="$self?$downRight"><img src="$STATIC_IMG_DIR/down-right.png" width="50" height="50" border="0"></a>
		-->
	</td>
</tr>

</table>

<p><a href="&#109;&#97;&#105;&#108;&#116;&#111;&#58;&#109;&#49;&#107;&#101;&#108;&#108;&#121;&#64;&#117;&#99;&#115;&#100;&#46;&#101;&#100;&#117;">Problems?</a></p>
<p><small><pre>$version</pre></small></p>

</body>
</html>
_HTML_



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
# Returns:
#	- query string
###################################################################
sub state{
	my ($from, $to, $x, $y, $scale, $size) = (@_);
	return "from=$from&amp;to=$to&amp;xoff=$x&amp;yoff=$y&amp;scale=$scale&amp;size=$size";
}

###################################################################
# Create a menu from an array of values, which, when selected, modifies the
# given form object. Uses what I think is the basic, cross-browser DOM. Should
# work (almost) everywhere.
# Confirmed to WORK in:
#	Mozilla, Firefox, Opera, Galeon
# Confirmed NOT TO WORK in:
#	lynx, links ;)
#
# Args:
#	- hashref of values to build menu from
#	- DOM name of target object to change (i.e., formname.inputname.value).
#	  Leave blank to skip this.
#	- the currently-selected value (value, not index)
# Returns:
#	- a <select> HTML element
###################################################################
sub buildMenu{
	my ($values, $target, $cur) = (@_);
	my($evalue, $dsplvalue);

	my $retStr = qq|<select>\n|;
	my $selected;
	foreach my $value (@$values){
		$selected = ($value eq $cur) ? ' selected="selected"' : '';
		# escape HTML; the 'None' location is a null string
		if($value eq 'None'){
			$evalue = '';
			$dsplvalue = '(None)';
		}
		else{
			$evalue = $dsplvalue = CGI::escapeHTML($value);
		}
		my $js = '';
		if($target ne ''){
			$js = qq| onClick="$target = '$evalue'"|;
		}
		$retStr .= qq|<option value="$evalue"$selected$js>| .
		           qq|$dsplvalue</option>\n|;
	}
	$retStr .= "</select>\n";
	return $retStr;
}

###################################################################
# Given numbers A, B, and C, returns the value that is as close to C as
# possible while still being in [A, B].
# Args:
#	- minimum value (A)
#	- maximum value (B)
#	- target value (C)
# Returns:
#	- Value in [A, B] that's closest to the target value.
###################################################################
sub between{
	my($min, $max, $val) = (@_);
	if($val < $min){
		return $min;
	}
	if($val > $max){
		return $max;
	}
	return $val;
}

###################################################################
# Return HTML for widget that shows the current zoom level, and allows an easy
# way to change it. (Does NOT print.)
# Args:
#	Identical to state().
# Returns:
#	- the HTML for the widget
###################################################################
sub zoomWidget{
	my ($from, $to, $x, $y, $scale, $size) = (@_);

	# generate "+" and "-" buttons
	my $zoomOut = $self . '?' . state($from, $to, $x, $y,
		($scale < $#MapGlobals::SCALES) ? $scale + 1 : $#MapGlobals::SCALES, $size);
	my $zoomIn  = $self . '?' . state($from, $to, $x, $y,
		($scale > 0) ? $scale - 1 : 0, $size);

	my $ret = qq|Zoom: <a href="$zoomOut">[-]</a> <a href="$zoomIn">[+]</a>|;
	#my $ret .= qq|<table border="0"><tr><td><a href="$zoomOut">[-]</a></td>|;
	$ret .= qq|<table border="0"><tr>|;

	my ($curState, $style);
	# print out all the zoom levels
	# we iterate in reverse because zooms are stored in order of decreasing
	# zoom internally (why? that's actually a good question. Not sure if
	# there's a reason. It may change.)
	for my $i (reverse(0..$#MapGlobals::SCALES)){
		$curState = $self . '?' . state($from, $to, $x, $y, $i, $size);
		$style = ($i == $scale) ? 'background: #CCFF00' : '';
		$ret .= qq|<td style="$style"><small>[<a href="$curState">| . ($MapGlobals::SCALES[$i]*100) . qq|%</a>]</small></td>|;
	}

	#$ret .= qq|<td><a href="$zoomIn">[+]</a></td></tr></table>|;
	$ret .= qq|</tr></table>|;

	return $ret;
}
