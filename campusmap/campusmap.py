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
from google.appengine.ext import db
from google.appengine.ext.webapp import template
from google.appengine.ext.webapp import util

import difflib
import logging
import os
import pickle
import re
import urllib

class Map:
    _locations_html = 'locations.html'
    _locations_dat = 'locations.pickle'

    main_tmpl = 'template.html'

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

    def fuzzyFind(self, s):
        """Try to find a matching location, or list of locations, by name or alias.
        
        Args:
            s (str): the search string
        
        Returns:
            (list) a list of (score, location) tuples.
        """
        ratio_cutoff = 0.4
        nonalnum = lambda x: not x.isalnum()
        matches = []

        for loc in locations['ByID'].values():
            if hasattr(loc, 'aliases'):
                aliases = loc['aliases']
            else:
                aliases = []
            for s in [loc['name']] + aliases:
                ratio = difflib.SequenceMatcher(nonalnum, search, s).ratio()
                if ratio >= ratio_cutoff:
                    matches.append((ratio, loc))
        matches.sort(lambda x, y: cmp(y[0], x[0]))

        return matches

class PathInfo(db.Model):
    x = db.IntegerProperty(required=True)
    y = db.IntegerProperty(required=True)
    w = db.IntegerProperty(required=True)
    h = db.IntegerProperty(required=True)
    dist = db.IntegerProperty(required=True)
    id0 = db.IntegerProperty(required=True)
    id1 = db.IntegerProperty(required=True)

    def __str__(self):
        return "<PathInfo %d %d: %dx%d@%d,%d (%d)>" % (self.id0, self.id1,
                self.w, self.h, self.x, self.y, self.dist)
    @staticmethod
    def fromSrcDst(src_id, dst_id):
        id0 = min(int(src_id), int(dst_id))
        id1 = max(int(src_id), int(dst_id))
        return PathInfo.gql('WHERE id0 = :1 AND id1 = :2', id0, id1).get()

class PathImage(db.Model):
    id0 = db.IntegerProperty(required=True)
    id1 = db.IntegerProperty(required=True)
    zoom = db.IntegerProperty(required=True) 
    image = db.BlobProperty()

    @staticmethod
    def fromPath(src_id, dst_id, zoom):
        id0 = min(int(src_id), int(dst_id))
        id1 = max(int(src_id), int(dst_id))
        zoom = int(zoom)
        return PathImage.gql('WHERE id0 = :1 AND id1 = :2 AND zoom = :3', id0, id1, zoom).get()
