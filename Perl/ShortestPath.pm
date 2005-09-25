# vim: tabstop=4 shiftwidth=4
# -----------------------------------------------------------------
# ShortestPath.pm -- Routines dealing with the algorithmic process of finding
# the shortest path to a given point on a graph.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# $Id$
# -----------------------------------------------------------------

package ShortestPath;

use strict;
use warnings;

use MapGlobals qw(TRUE FALSE INFINITY min max plog);
use Heap::Elem::GraphPoint;
use Heap::Elem::Dijkstra;
use Heap::Fibonacci;

###################################################################
# Create a hash of minimum distances with Dijkstra's algorithm.
# Args:
#	- the ID of the GraphPoint from which paths should be calculated.
#	- a hashref of GraphPoints to search over. this is also an output
#	  parameter: the 'Distance' and 'From' fields are set to usable values.
# Returns: 
#	- nothing, but the hashref of points is altered.
###################################################################
sub find{
	my($startID, $points) = (@_);

	plog("Running Dijkstra's algorithm.\n");

	my($s, $v, $w);
	my($vp, $wp);
	my $smallestIndex;
	my $connID;
	# this should be bigger than any other "real" distance, yet it should be small
	# enough that any other real distance plus this one does NOT cause integer
	# overflow.
	my $KONSTANT = 1e7;
	# this is either 0 or $KONSTANT, depending on context
	my $K;

	#my $fibheap = makeMinHeap($weights);
	my $weights = makeWeightHash($points);
	my $fibheap = makeMinHeap($weights);

	# translate the starting ID into an actual graph point
	$s = $points->{$startID};
	# the start location is quite close to itself :)
	$weights->{$startID}{'Distance'} = 0;

	#warn "Dijkstra's algorithm:\n";

	while(1){
		# get the smallest unknown vertex.
		# we end when we run out of elements in the tree.
		# $v is a Dijkstra cache object
		# $vp is a GraphPoint
		if( !defined( $v = $fibheap->extract_top() ) ){
			#warn "oh noes! we're done!\n";
			last;
		}
		$vp = $points->{$v->{'PointID'}};

		# $v is now a known vertex
		$v->{'Known'} = TRUE;

		# loop over all points adjacent to $v
		my @conns = keys %{$points->{$v->{'PointID'}}{'Connections'}};
		#warn "Connections: (@conns)\n";
		for $connID ( @conns ){
			# assign the adjacent point to $w
			# $w is a Dijkstra cache object
			# $wp is a GraphPoint
			$w = $weights->{$connID};
			$wp = $points->{$w->{'PointID'}};

			# if $w hasn't been visited yet
			if( !$w->{'Known'} ){
				# $K is non-zero only if the location is no-pass-through
				$K = ( $wp->{'PassThrough'} == 0 ) ? $KONSTANT : 0;

				# if we found a shorter distance...
				if($v->{'Distance'} + $K
					+ $vp->{'Connections'}{$connID}{'Weight'} < $w->{'Distance'}) {

					# reset the distance to this lower value
					$w->{'Distance'} = $v->{'Distance'} + $K
						+ $vp->{'Connections'}{$connID}{'Weight'};

					# keep track where we came from
					$w->{'From'} = $v->{'PointID'};

					# we changed the value, so the tree must be notified
					$fibheap->decrease_key($w);
				}
			}
		}
	}

	#warn "POST-DIJKSTRA:\n";
	#foreach( values %$weights ){
	#	warn "$_->{'PointID'}: d = $_->{'Distance'}, f = $_->{'From'}\n";
	#	if( !defined($_->{'From'}) && $_->{'Distance'} != INFINITY){
	#		warn "^^^ THIS IS THE START LOCATION.\n";
	#	}
	#}

	return $weights;
}

###################################################################
# Make a hash of Dijkstra objects to hold the 'distance' and 'from' pointers of
# each GraphPoint.
# Args: none
# Returns:
#	- a hashref of virgin Dijkstra objects, indexed by GraphPoint ID
###################################################################
sub makeWeightHash{
	my($points) = (@_);
	my $weights = {};
	foreach ( values %$points ){
		$weights->{$_->{'ID'}} = Heap::Elem::Dijkstra->new(
			PointID => $_->{'ID'},
			Distance => INFINITY,
			From => 0,
			Known => FALSE,
		);
	}
	return ($weights);
}

###################################################################
# Create a new heap (a Fibonacci tree) containing all the GraphPoints given.
# Args:
#	- a hashref of GraphPoints to add to the tree
# Returns:
#	- a Heap::Fibonacci object containing all the given GraphPoints (shall
#	  I say that once more?)
###################################################################
sub makeMinHeap{
	my($points) = (@_);
	my $fib = Heap::Fibonacci->new();
	foreach ( values %$points ){
		$fib->add($_);
	}
	return $fib;
}

###################################################################
# Given a hashref of points that has been run through ShortestPath::find(),
# print the shortest path to a given point. (The 'source' locatiion was given
# to find() to create the hashref of GraphPoints.)
#
# XXX: Does this even still work? I haven't used it in a long time.
# TODO: Grep for any calls to this function (there shouldn't be any), and remove it.
#
# Args:
#	- a hashref of GraphPoint objects
#	- a reference to the target Graphpoint
#	- a string to prepend to the output
# Returns:
#	- a string describing the path
###################################################################
sub pathTo{
	my $points = shift;
	my $target = shift;
	my $str = shift || '';
	
	if( defined($target->{'From'}{'ID'}) ){
		$str .= pathTo($points, $target->{'From'});
		$str .= " to ";
	}
	$str .= "$target->{'ID'}";
	return $str;
}

###################################################################
# Collect the coordinates of all points along a path and calculate the viewing
# rectangle necessary to view the entire path, as well as the distance of the
# path (in pixels).
# Args:
#	- a hashref of GraphPoints
#	- the distances and from pointers set by Dijkstra's algorithm
#	- hashref to to the 'source' GraphPoint
#	- hashref to to the 'destination' GraphPoint
# Returns:
#	- the distance of the path OR undef if the two points are not connected.
#	- a hashref specifying the two corners of the viewing rectangle necessary to view the path:
#	  Keys: (xmin, ymin, xmax, ymax)
#	- an arrayref of arrayrefs of point hashrefs (containing 'x' and 'y') keys.
#	  Each sub-arrayref represents a single Edge. This data structure can
#	  be fed to MapGraphics::drawLines().
###################################################################
sub pathPoints{
	my($points, $weights, $source, $target) = (@_);

	# target Dijkstra cache object
	my $td = $weights->{$target->{'ID'}};

	my $dist = 0;
	my @pathPoints;

	my $xmin = min($source->{'x'}, $target->{'x'});
	my $xmax = max($source->{'x'}, $target->{'x'});
	my $ymin = min($source->{'y'}, $target->{'y'});
	my $ymax = max($source->{'y'}, $target->{'y'});

	# abort right now if we find a disconnected node
	if( $td->{'Distance'} == INFINITY ){
		return(
			undef, 
			{ xmin => $xmin, ymin => $ymin, xmax => $xmax, ymax => $ymax },
			[]
		);
	}

	# follow 'from' links until we reach the original point
	my $conn;
	while( $td->{'From'} ){
		my $subPath = [];

		$conn = $target->{'Connections'}{$td->{'From'}};
		my $thisEdge = LoadData::loadEdge($conn->{'EdgeID'});
		$dist += $conn->{'Weight'};

		# keep following the trail back to its source
		$target = $points->{$td->{'From'}};
		$td = $weights->{$target->{'ID'}};

		# cycle through each point in this edge
		foreach my $curpt ( @{$thisEdge->{'Path'}} ){

			# keep the min/max values up to date
			$xmax = $curpt->{'x'} if( $curpt->{'x'} > $xmax );
			$xmin = $curpt->{'x'} if( $curpt->{'x'} < $xmin );
			$ymax = $curpt->{'y'} if( $curpt->{'y'} > $ymax );
			$ymin = $curpt->{'y'} if( $curpt->{'y'} < $ymin );
			
			push(@$subPath, { x => $curpt->{'x'}, y => $curpt->{'y'} });
		}

		push(@pathPoints, $subPath);
	}

	return (
		$dist,
		{ xmin => $xmin, ymin => $ymin, xmax => $xmax, ymax => $ymax },
		\@pathPoints
	);
}

###################################################################
# Find the distance to a given point.
#
# NOTE: We need this function because the raw distance value in each GraphPoint
# includes fake adjustments for the no-pass-through flag. I think.
#
# Args:
#	- a hashref of points, run through find(). The arguments to find()
#	  determine how this is generated, and thus what the "source" location is.
#	- a hashref to the "destination" location.
# Returns:
#	- the shortest distance between the two points, in pixels.
###################################################################
sub distTo{
	my($points, $weights, $target) = (@_);
	my $dist = 0;

	while( $weights->{$target->{'ID'}}{'From'} ){
		$dist += $target->{'Connections'}{$weights->{$target->{'ID'}}{'From'}}{'Weight'};
		# keep following the trail back to its source
		$target = $points->{$weights->{$target->{'ID'}}{'From'}};
	}
	return $dist;
}

###################################################################
# Given an edge, its bounding rectangle and extract its points.
#
# XXX: Is this called from anywhere? I think it's an orphan. I'll kill it once
# I'm sure.
#
# Args:
#	- a hashref to said edge
# Returns:
#	- bounding rectangle of said edge. Same as second return value of
#	  pathPoints().
#	- an arrayref of all the points in the edge (stored as hashrefs with
#	  'x' and 'y' keys).
###################################################################
sub edgePoints{
	my($edge) = @_;
	my @points;

	my($xmin, $xmax, $ymin, $ymax);

	# cycle through each point in this edge
	foreach my $curpt ( @{$edge->{'path'}} ){

		# keep the min/max values up to date
		$xmax = $curpt->{'x'} if( !defined($xmax) || $curpt->{'x'} > $xmax );
		$xmin = $curpt->{'x'} if( !defined($xmin) || $curpt->{'x'} < $xmin );
		$ymax = $curpt->{'y'} if( !defined($ymax) || $curpt->{'y'} > $ymax );
		$ymin = $curpt->{'y'} if( !defined($ymin) || $curpt->{'y'} < $ymin );
		
		push(@points, { x => $curpt->{'x'}, y => $curpt->{'y'} });
	}

	return (
		{ xmin => $xmin, ymin => $ymin, xmax => $xmax, ymax => $ymax },
		\@points,
	);
}

1;
