#!/usr/bin/perl
# -----------------------------------------------------------------
# map.cgi -- An interface wrapper for shortest.cgi.
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Sat Mar 26 13:54:50 PST 2005
# -----------------------------------------------------------------

use strict;
use warnings;

use CGI;
use MapGlobals;
use LoadData;

# the name of this script, for form actions, etc
my $self = 'map.cgi';
my $imagedir = '../..';

my $q = CGI->new();

# width and height of the viewing window (total image size is in MapGlobals)
my $width  = 500;
my $height = 400;

# how far (in pixels) to pan when the user clicks "left", "right", "up", or
# "down"
my $pan = 100;

# source and destination location names
my $fromTxt = $q->param('from') || '';
my $toTxt   = $q->param('to')   || '';

# HTML-safe versions of the from and to text
my $fromTxtSafe = CGI::escapeHTML($fromTxt);
my $toTxtSafe = CGI::escapeHTML($toTxt);

# URL-safe versions of the from and to text
my $fromTxtURL = CGI::escape($fromTxt);
my $toTxtURL = CGI::escape($toTxt);

# x and y display offsets, from the upper left
my $xoff   = int($q->param('xoff')   || 0);
my $yoff   = int($q->param('yoff')   || 0);
my $scale  = $q->param('scale')      || 1;

# click offsets from the map
# (these are the only two input variables that could be undefined)
my $mapx = defined($q->param('map.x')) ? int($q->param('map.x')) : undef;
my $mapy = defined($q->param('map.y')) ? int($q->param('map.y')) : undef;

# get all the locations off disk
my $locations = LoadData::loadLocations($MapGlobals::LOCATION_FILE);

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
my $fromMenu = buildMenu(\@locNames, 'main.from.value');
my $toMenu = buildMenu(\@locNames, 'main.to.value');

# now convert $fromTxt and $toTxt to IDs
my $ERROR = '';
my ($from, $to) = (0, 0);

if( defined($locations->{'name:' . LoadData::nameNormalize($toTxt)}{'ID'}) ){
	$to = $locations->{'name:' . LoadData::nameNormalize($toTxt)}{'ID'};

	# set the offsets to the coords of the 'to' location
	if(!$xoff && !$yoff){
		warn "setting x/y offsets (TO)\n";
		$xoff = $locations->{$to}{'x'};
		$yoff = $locations->{$to}{'y'};
	}
}
else{
	$ERROR .= "<b>Location &quot;$toTxtSafe&quot; not found.</b>\n";
}

if( defined($locations->{'name:' . LoadData::nameNormalize($fromTxt)}{'ID'}) ){
	$from = $locations->{'name:' . LoadData::nameNormalize($fromTxt)}{'ID'};

	# this is only here so that, if the 'to' location doesn't exist,
	# we set the offsets to _something_ reasonable
	if(!$xoff && !$yoff){
		warn "setting x/y offsets (FROM)\n";
		$xoff = $locations->{$from}{'x'};
		$yoff = $locations->{$from}{'y'};
	}
}
else{
	$ERROR .= "<b>Location &quot;$fromTxtSafe&quot; not found.</b>\n";
}

if( defined($mapx) && defined($mapy) ){
	# adjust the map offsets so they work from the center of the screen
	$mapx -= $width/2;
	$mapy -= $height/2;

	# now adjust our absolute x/y offsets to take into account the last click location
	$xoff += $mapx;
	$yoff += $mapy;
}


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
	<a href="$self?$zoomIn">[+]</a> -- 
	<a href="$self?$zoomOut">[-]</a>
	<a href="$self?$up"><img src="$imagedir/up.png" width="50" height="50" border="0"></a><br />
	</td>
</tr>

<tr>
	<td valign="center">
		<a href="$self?$left"><img src="$imagedir/left.png" width="50" height="50" border="0"></a>
	</td>

	<td valign="center">
		<form method="get" action="$self" target="_self">
		<input type="hidden" name="xoff" value="$xoff" />
		<input type="hidden" name="yoff" value="$yoff" />
		<input id="from" type="hidden" name="from" value="$fromTxtSafe" />
		<input id="to" type="hidden" name="to" value="$toTxtSafe" />
		<input type="image" name="map" width="$width" height="$height" border="0"
			src="shortest.cgi?$curState" />
		</form>
	</td>

	<td valign="center">
		<a href="$self?$right"><img src="$imagedir/right.png" width="50" height="50" border="0"></a>
	</td>
</tr>

<tr>
	<td colspan="3" align="center">
		<a href="$self?$down"><img src="$imagedir/down.png" width="50" height="50" border="0"></a>
	</td>
</tr>

</table>

<form method="get" action="$self" target="_self" name="main">
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

sub state{
	my ($from, $to, $x, $y, $scale) = (@_);
	return "from=$from&amp;to=$to&amp;xoff=$x&amp;yoff=$y&amp;scale=$scale";
}

sub buildMenu{
	my ($values, $target) = (@_);
	my $evalue;

	my $retStr = qq|<select>\n|;
	foreach my $value (@$values){
		$evalue = CGI::escapeHTML($value);
		$retStr .= qq|<option value="$evalue" onClick="$target = '$evalue'">| .
		           qq|$evalue</option>\n|;
	}
	$retStr .= "</select>\n";
}
