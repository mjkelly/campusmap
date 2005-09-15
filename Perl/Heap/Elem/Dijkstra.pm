# -----------------------------------------------------------------
# Dijkstra.pm -- A simple object used for storing the GraphPoint attributes
# that may be modified by Dijkstra's algorithm. Loadable and saveable
# independently of the GraphPoints themselves.
#
# Copyright 2005 Michael Kelly and David Lindquist
#
# Tue Sep 13 21:05:55 PDT 2005
# -----------------------------------------------------------------

package Heap::Elem::Dijkstra;

use warnings;
use strict;

require Exporter;

use vars qw(@ISA);
@ISA = qw(Exporter Heap::Elem);

# create a new object from a hash. standard, generic constructor.
sub new {
	my $class = shift();
	$class = ref($class) || $class;

	my $self = { @_, heap => undef };

	return bless($self, $class);
}

# compare two objects based on their distance from the source point
sub cmp {
	return shift()->{'Distance'} <=> shift()->{'Distance'};
}

# get or set the 'value' of a objects (its distance from the source point)
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
