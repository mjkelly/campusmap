#!/usr/bin/python
# -----------------------------------------------------------------
# importers.py -- Helper functions for dealing with the bulkloader.
# Copyright 2010 Michael Kelly (michael@michaelkelly.org)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Thu Aug  5 00:27:10 EDT 2010
# -----------------------------------------------------------------

import campusmap

import os

PATH_DIR=os.getenv('HOME') + '/src/git/campusmap-data/html/dynamic/paths'

def PathImageImporter(input_dict, instance, bulkload_state_copy):
    image_path = PATH_DIR + '/im-%s-%s-%s.png' % (input_dict['id0'], input_dict['id1'], input_dict['zoom'])
    fh = file(image_path)
    image = fh.read()
    fh.close()
    return campusmap.PathImage(id0=int(input_dict['id0']),
                               id1=int(input_dict['id1']),
                               zoom=int(input_dict['zoom']),
                               image=image)
