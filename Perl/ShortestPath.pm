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

use MapGlobals qw(TRUE FALSE INFINITY min max);
#use MapGraphics;
use Heap::Elem::GraphPoint;
use Heap::Fibonacci;

#use GD;

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

	my($s, $v, $w);
	my $smallestIndex;
	my $connID;
	# this should be bigger than any other "real" distance, yet it should be small
	# enough that any other real distance plus this one does NOT cause integer
	# overflow.
	my $KONSTANT = 1e7;

	# translate the starting ID into an actual graph point
	$s = $points->{$startID};

	my $fibheap = makeMinHeap($points);

	$s->{'Distance'} = 0;

	while(1){
		# get the smallest unknown vertex.
		# we end when we run out of elements in the tree.
		if( !defined( $v = $fibheap->extract_top() ) ){
			last;
		}
		# $v is now a known vertex
		$v->{'Known'} = TRUE;

		# loop over all points adjacent to $v
		for $connID ( keys %{$v->{'Connections'}} ){
			# assign the adjacent point to $w
			# $w is a GraphPoint
			$w = $points->{$connID};

			# if $w hasn't been visited yet
			if( !$w->{'Known'} ){
				if( $w->{'PassThrough'} == 0 )
				{
					# if v's distance + the distance between v and w is less
					# than w's distance
					if($v->{'Distance'} + $KONSTANT + $v->{'Connections'}{$connID}{'Weight'} <
						$w->{'Distance'})
					{
						$w->{'Distance'} = $v->{'Distance'} + $KONSTANT
							+ $v->{'Connections'}{$connID}{'Weight'};

						# indicate where we got this path
						$w->{'From'} = $v;

						# we changed the value, so the tree must be notified
						$fibheap->decrease_key($w);
					}
				}
				else{
					# if v's distance + the distance between v and w is less
					# than w's distance
					if($v->{'Distance'} + $v->{'Connections'}{$connID}{'Weight'} <
						$w->{'Distance'})
					{
						$w->{'Distance'} = $v->{'Distance'}
							+ $v->{'Connections'}{$connID}{'Weight'};

						# indicate where we got this path
						$w->{'From'} = $v;

						# we changed the value, so the tree must be notified
						$fibheap->decrease_key($w);
					}
				}
			}

		}

	}
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
#	- a hashref of GraphPoints that has been modified by
#	  ShortestPath::find() (i.e., the 'Distance' and 'From' fields have been
#	  set)
#	- an open filehandle to the Edge file (from LoadData::initEdgeFile())
#	- the length of each edge record (also from LoadData::initEdgeFile())
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
	my($points, $edgeFH, $edgeSize, $source, $target) = (@_);

	my $dist = 0;
	my @pathPoints;

	my $xmin = min($source->{'x'}, $target->{'x'});
	my $xmax = max($source->{'x'}, $target->{'x'});
	my $ymin = min($source->{'y'}, $target->{'y'});
	my $ymax = max($source->{'y'}, $target->{'y'});

	# abort right now if we find a disconnected node
	if( $target->{'Distance'} >= INFINITY ){
		return(
			undef, 
			{ xmin => $xmin, ymin => $ymin, xmax => $xmax, ymax => $ymax },
			[]
		);
	}

	# follow 'from' links until we reach the original point
	my $conn;
	while( defined($target->{'From'}{'ID'}) ){
		my $subPath = [];

		$conn = $target->{'Connections'}{$target->{'From'}{'ID'}};
		#my $thisEdge = $edges->{$conn->{'EdgeID'}};
		my $thisEdge = LoadData::loadEdge( $edgeFH, $edgeSize, $conn->{'EdgeID'} );
		$dist += $conn->{'Weight'};

		# keep following the trail back to its source
		$target = $target->{'From'};

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
	my($points, $target) = (@_);
	my $dist = 0;

	while( defined($target->{'From'}{'ID'}) ){
		$dist += $target->{'Connections'}{$target->{'From'}{'ID'}}{'Weight'};
		# keep following the trail back to its source
		$target = $target->{'From'};
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
