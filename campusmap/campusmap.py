#!/usr/bin/env python
#
# Copyright 2010 David Lindquist and Michael Kelly
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from google.appengine.ext import webapp
from google.appengine.ext.webapp import template
from google.appengine.ext.webapp import util
import logging
import os
import urllib
import re
import pickle

class Map:
    _locations_html = 'locations.html'
    _locations_dat = 'locations.pickle'

    main_tmpl = 'js_tmpl.html'

    default_xoff  = 4608
    default_yoff  = 3172

    # default zoom level
    default_scale = 3
    # default viewport size (index into old defaults; not currently used)
    default_size = 1
    # default walk sped (minutes per mile)
    default_mpm = 20

    html_base = '/static'

    def __init__(self):
        fh = open(self._locations_html)
        self.locations_menu = fh.read()
        fh.close()

        fh = open(self._locations_dat)
        self.locations = pickle.load(fh)
        fh.close()

    def isKeyword(self, s):
        """Return if s is a location keyword.

        E.g., closest food, closest parking, etc.
        """
        return s.lower().startswith('keyword:')
        
    def nameNormalize(self, s):
        """Normalize s: remove all non-alphanumeric and lowercase."""
        return re.sub(r'\W', '', 'fo bar:').lower()

    def findLocation(self, s):
        # First try the lookup tables.
        if not s:
            return None
        try:
            if int(s) in self.locations['ByID']:
                found = self.locations['ByID'][int(s)]
                logging.info("Found location by ID: %r = %s", s, found)
                return found
        except ValueError:
            pass

        if s in self.locations['ByCode']:
            found = self.locations['ByCode'][s]
            logging.info("Found location by code: %r = %s", s, found)
            return found
        else:
            logging.info("Can't find location: %r", s)

        # Now search the entire list.
        all_locs = self.locations['ByID'].values()
        found = [loc for loc in all_locs if str(loc['name']) == str(s)]
        if len(found) == 1:
            logging.info("Found location by exact name match: %r = %s", s, found[0])
            return found[0]

    def loadPathInfo(self, src_id, dst_id):
        return {'x': 0,
                'y': 0,
                'w': 50,
                'h': 50,
                'dist': 100,
                'src': src_id,
                'dst': dst_id}
