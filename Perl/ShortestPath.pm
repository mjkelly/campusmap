# -----------------------------------------------------------------
# ShortestPath.pm -- Routines dealing with the algorithmic process of finding
# the shortest path to a given point on a graph.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Mon Mar 28 19:50:39 PST 2005
# -----------------------------------------------------------------

package ShortestPath;

use strict;
use warnings;

use MapGlobals qw(TRUE FALSE INFINITY min max);
use MapGraphics;
use Heap::Elem::GraphPoint;
use Heap::Fibonacci;

use GD;

# create a hash of minimum distances with Dijkstra's algorithm
# XXX: proper desc. and function header
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

			#XXX: print STDERR "Adjacent node ID: $connID\n";

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

# create a new heap (a Fibonacci tree) containing all the GraphPoints
sub makeMinHeap{
	my($points) = (@_);
	my $fib = Heap::Fibonacci->new();
	foreach ( values %$points ){
		$fib->add($_);
	}
	return $fib;
}

# given a data structure populated by shortestPath(), find the shortest
# path to a given point ID.
# XXX: proper desc. and function header
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

# write the path to a given target point, given a hashref of post-Dijkstra
# points, a hashref of edges, a GD image to draw to, and a color to draw with.
# returns pixel distance between the two points
# XXX: proper desc. and function header
sub drawTo{
	my($points, $edges, $target, $im, $color, $xoff, $yoff, $w, $h, $scale) = (@_);

	my $dist = 0;

	my %rect;
	my($xmin, $xmax, $ymin, $ymax);

	# follow 'from' links until we reach the original point
	my $conn;
	while( defined($target->{'From'}{'ID'}) ){

		$conn = $target->{'Connections'}{$target->{'From'}{'ID'}};
		$dist += $conn->{'Weight'};
		# keep following the trail back to its source
		$target = $target->{'From'};

		# this is all housekeeping for the smart zoom
		%rect = MapGraphics::drawEdge(
			$edges->{$conn->{'EdgeID'}}, $im, 2, $color,
			$xoff, $yoff, $w, $h, $scale, 1);
		$xmax = $rect{'xmax'} if( !defined($xmax) || $rect{'xmax'} > $xmax );
		$xmin = $rect{'xmin'} if( !defined($xmin) || $rect{'xmin'} < $xmin );
		$ymax = $rect{'ymax'} if( !defined($ymax) || $rect{'ymax'} > $ymax );
		$ymin = $rect{'ymin'} if( !defined($ymin) || $rect{'ymin'} < $ymin );
	}

	return ($dist, { xmin => $xmin, ymin => $ymin, xmax => $xmax, ymax => $ymax });
}

# collect the coords of all points along a path and remember the maximum and minimum values
# XXX: proper desc. and function header
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

# find the distance to a given point
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
