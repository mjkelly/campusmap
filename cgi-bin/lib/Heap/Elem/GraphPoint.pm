# -----------------------------------------------------------------
# GraphPoint.pm -- Wrap GraphPoints in a Perl object, so they 
# can be put in all kinds of fancy data structures that we don't want to write
# ourselves (read: Fibonacci trees). This whole data structure is tailored for
# use in Dijkstra's algorithm. It may not make the most sense for GraphPoints
# in general.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Sun Jul 17 19:55:38 PDT 2005
# -----------------------------------------------------------------

package Heap::Elem::GraphPoint;

use warnings;
use strict;

require Exporter;

use vars qw(@ISA);
@ISA = qw(Exporter Heap::Elem);

# create a new GraphPoint from a hash.
sub new {
	my $class = shift();
	$class = ref($class) || $class;

	my $self = { @_, heap => undef };

	return bless($self, $class);
}

# compare two GraphPoints based on their distance from the source point
sub cmp {
	return shift()->{'Distance'} <=> shift()->{'Distance'};
}

# get or set the 'value' of a GraphPoint (its distance from the source point)
sub val {
    my $self = shift;
    if(@_){
    	return $self->{'Distance'} = shift();
    }
    else{
    	return $self->{'Distance'};
    }
}

# get or set the internal value used by the heap
sub heap {
    my $self = shift;
    if(@_){
    	return $self->{'heap'} = shift();
    }
    else{
    	return $self->{'heap'};
    }
}

1;
