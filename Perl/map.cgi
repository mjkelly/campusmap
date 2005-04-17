#!/usr/bin/perl
# -----------------------------------------------------------------
# newmap.cgi -- The user interface for the UCSDMap.
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Sat Apr 16 00:23:35 PDT 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use CGI;
use MapGlobals;
use LoadData;
use MapGraphics;
use ShortestPath;
use File::Temp ();

my $q = CGI::new();

# the name of this script, for form actions, etc
my $self = 'map.cgi';

# width and height of the viewing window (total image size is in MapGlobals)
my $width  = 500;
my $height = 400;

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

# URL-safe versions of the from and to text
my $fromTxtURL = CGI::escape($fromTxt);
my $toTxtURL = CGI::escape($toTxt);

# normalized versions of the from and to text
my $fromTxtLookup = LoadData::nameLookup($fromTxt);
my $toTxtLookup = LoadData::nameLookup($toTxt);

# x and y display offsets, from the upper left
my $xoff   = int($q->param('xoff')   || 0);
my $yoff   = int($q->param('yoff')   || 0);
my $scale  = $q->param('scale')      || 1;

# click offsets from the map
# (these are the only two input variables that could be undefined)
my $mapx = defined($q->param('map.x')) ? int($q->param('map.x')) : undef;
my $mapy = defined($q->param('map.y')) ? int($q->param('map.y')) : undef;

# clear out any old images
MapGlobals::reaper($MapGlobals::DYNAMIC_MAX_AGE, $MapGlobals::DYNAMIC_IMG_SUFFIX);

# build a list of printable location names
my @locNames = ();
foreach (sort keys %$locations){
	# add each unique name
	if( substr($_, 0, 5) eq 'name:' ){
		push(@locNames, $locations->{$_}{'Name'});
	}
}
# build menus to change the values of the form fields
# (These menus have no names, so they shouldn't submit any form data)
my $fromMenu = buildMenu(\@locNames, 'main.from.value', $fromTxt);
my $toMenu = buildMenu(\@locNames, 'main.to.value', $toTxt);

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
	else{
		$ERROR .= "<b>Destination location &quot;$toTxtSafe&quot; not found.</b>\n"
			if($toTxt ne '');
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
	else{
		$ERROR .= "<b>Start location &quot;$fromTxtSafe&quot; not found.</b>\n"
			if($fromTxt ne '');
		$src_found = FALSE;
	}

}

# if we still don't have offsets, use the default ones
if(!$xoff && !$yoff){
	$xoff = $MapGlobals::DEFAULT_XOFF;
	$yoff = $MapGlobals::DEFAULT_YOFF;
}

# we actually run the shortest-path algorithm only if we found both
# the source and destination locations
my $path = ($src_found && $dst_found);

# integrate the image click offsets (if the image was clicked to recenter)
if( defined($mapx) && defined($mapy) ){
	# adjust the map offsets so they work from the center of the screen
	$mapx -= $width/2;
	$mapy -= $height/2;

	# now adjust our absolute x/y offsets to take into account the last click location
	$xoff += $mapx/$scale;
	$yoff += $mapy/$scale;
}

# make sure the x/y offsets are in range, and correct them if they aren't
$xoff = between(($width/$scale)/2, $MapGlobals::IMAGE_X - ($width/$scale)/2, $xoff);
$yoff = between(($height/$scale)/2, $MapGlobals::IMAGE_Y - ($height/$scale)/2, $yoff);

my $curState = state($from, $to, $xoff, $yoff, $scale);

# states for the directions
my $left  = state($fromTxtURL, $toTxtURL, $xoff - $pan/$scale, $yoff, $scale);
my $right = state($fromTxtURL, $toTxtURL, $xoff + $pan/$scale, $yoff, $scale);
my $up    = state($fromTxtURL, $toTxtURL, $xoff, $yoff - $pan/$scale, $scale);
my $down  = state($fromTxtURL, $toTxtURL, $xoff, $yoff + $pan/$scale, $scale);

# states for zoom in/out
my $zoomOut  = state($fromTxtURL, $toTxtURL, $xoff, $yoff,
	($scale/2 >= MIN_SCALE) ? $scale/2 : MIN_SCALE);
my $zoomIn = state($fromTxtURL, $toTxtURL, $xoff, $yoff,
	($scale*2 <= MAX_SCALE) ? $scale*2 : MAX_SCALE);


# now do the actual pathfinding (maybe)...

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

my $rawxoff = $xoff*$scale - $width/2;
my $rawyoff = $yoff*$scale - $height/2;

# make sure the offsets don't go out of range
$rawxoff = between(0, $MapGlobals::IMAGE_X - $width, $rawxoff);
$rawyoff = between(0, $MapGlobals::IMAGE_Y - $height, $rawyoff);

my $im = GD::Image->newFromGd2Part(MapGlobals::getGd2Filename($scale),
	$rawxoff, $rawyoff, $width, $height)
	|| die "Could not load image $MapGlobals::BASE_GD2_IMAGE\n";

my $red = $im->colorAllocate(255, 0, 0);
my $green = $im->colorAllocate(0, 255, 0);


#MapGraphics::drawAllEdges($edges, $im, 1, $red, $rawxoff, $rawyoff, $width, $height, $scale);
MapGraphics::drawAllLocations($locations, $im, $red, $red, $rawxoff, $rawyoff,
				$width, $height, $scale);

# do the shortest path stuff
if($path){
	ShortestPath::find($startID, $points);
	ShortestPath::drawTo($points, $edges, $points->{$endID}, $im, $green,
		$rawxoff, $rawyoff, $width, $height, $scale);
}

binmode($tmpfile);
print $tmpfile $im->png();


# now put everthing into a simple output template
# TODO: separate this from the code, maybe?
print <<_HTML_;
Content-type: text/html

<head>
<title>UCSD Map</title>
<meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
</head>
<body>

$ERROR

<table border="0" cellpadding="0" cellspacing="0">
<tr>
	<td colspan="3" align="center">

	<b><a href="$self?$zoomIn">[+]</a> -- 
	<a href="$self?$zoomOut">[-]</a></b><br />

	<a href="$self?$up"><img src="$STATIC_IMG_DIR/up.png" width="50" height="50" border="0"></a><br />
	</td>
</tr>

<tr>
	<td valign="center">
		<a href="$self?$left"><img src="$STATIC_IMG_DIR/left.png" width="50" height="50" border="0"></a>
	</td>

	<td valign="center">
		<form method="get" action="$self" target="_self">

		<input type="hidden" name="xoff" value="$xoff" />
		<input type="hidden" name="yoff" value="$yoff" />
		<input type="hidden" name="scale" value="$scale" />

		<input id="from" type="hidden" name="from" value="$fromTxtSafe" />
		<input id="to" type="hidden" name="to" value="$toTxtSafe" />
		<input type="image" name="map" width="$width" height="$height" border="0"
			src="$fname" />
		</form>
	</td>

	<td valign="center">
		<a href="$self?$right"><img src="$STATIC_IMG_DIR/right.png" width="50" height="50" border="0"></a>
	</td>
</tr>

<tr>
	<td colspan="3" align="center">
		<a href="$self?$down"><img src="$STATIC_IMG_DIR/down.png" width="50" height="50" border="0"></a>
	</td>
</tr>

</table>

<form method="get" action="$self" target="_self" name="main">
<input type="hidden" name="scale" value="$scale" />
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

</body>
</html>
_HTML_



# XXX: proper desc. and function header
sub state{
	my ($from, $to, $x, $y, $scale) = (@_);
	return "from=$from&amp;to=$to&amp;xoff=$x&amp;yoff=$y&amp;scale=$scale";
}

# create a menu from an array of values, which modifies the given form object,
# through the basic, cross-browser DOM.
# XXX: proper desc. and function header
sub buildMenu{
	my ($values, $target, $cur) = (@_);
	my $evalue;

	my $retStr = qq|<select>\n|;
	my $selected;
	foreach my $value (@$values){
		$selected = ($value eq $cur) ? 'selected="selected"' : '';
		$evalue = CGI::escapeHTML($value);
		$retStr .= qq|<option value="$evalue" $selected onClick="$target = '$evalue'">| .
		           qq|$evalue</option>\n|;
	}
	$retStr .= "</select>\n";
	return $retStr;
}

# given A, B, and C, returns the value that is as close to C as possible while
# still being in [A, B].
# XXX: proper desc. and function header
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

# EOF
