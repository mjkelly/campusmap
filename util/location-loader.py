#!/usr/bin/python
# -----------------------------------------------------------------
# loader.py -- $desc$
# Copyright 2010 Michael Kelly (michael@michaelkelly.org)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Sun Jul 25 22:07:26 EDT 2010
# -----------------------------------------------------------------

import pickle

fh = open('locations.pickle', 'r')
loc_lookup = pickle.load(fh)
fh.close()

print loc_lookup
