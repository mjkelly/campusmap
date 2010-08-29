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

    # in base map pixels
    default_xoff  = 4608
    default_yoff  = 3172
    # how many pixels (on the base map) equal one mile
    pixels_per_mile = 3894

    # Size multipliers by zoom level, compared to the scale used by locations
    # and PathInfo objects.
    scales = [0.5, 0.25, 0.125, 0.0625]

    # default zoom level
    default_scale = 3
    # default zoom level if we're only showing one location
    default_single_loc_scale = 1
    # default viewport size (index into old defaults; not currently used)
    # TODO: remove this entirely
    default_size = 1
    # default walk sped (minutes per mile)
    default_mpm = 20

    # Size (in actual on-screen pixels) of the viewport.
    viewport_w = 600
    viewport_h = 475

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
        return re.sub(r'\W', '', s).lower()

    def findLocation(self, s):
        # First try the lookup tables.
        if not s:
            return []
        try:
            if int(s) in self.locations['ByID']:
                found = self.locations['ByID'][int(s)]
                logging.info("Found location by ID: %r = %s", s, found)
                return [found]
        except ValueError:
            pass

        if s.lower() in self.locations['ByCode']:
            found = self.locations['ByCode'][s]
            logging.info("Found location by code: %r = %s", s, found)
            return [found]

        # Now search the entire list.
        all_locs = self.locations['ByID'].values()
        found = [loc for loc in all_locs if str(loc['name']) == str(s)]
        if len(found) == 1:
            logging.info("Found location by exact name match: %r = %s", s, found[0])
            return [found[0]]

        if self.isKeyword(s):
            keyword = s.lower()[len('keyword:'):].strip()
            if self.locations['ByKeyword'].has_key(keyword):
                return self.locations['ByKeyword'][keyword]
            else:
                return []

        # Last, fall back to fuzzy matching.
        found = self._fuzzyFind(s)
        # According to the python docs, a ratio >0.6 is a "good match".
        # But, in practice, a higher value seems necessary.
        if found and found[0][0] > 0.8:
            logging.info('Top location by fuzzy match: %r = %r', s, found[0])
            return [found[0][1]]
        else:
            logging.info('Top %d locations by fuzzy match for: %r = %r', len(found), s, found)
            return [r[1] for r in found]

        logging.info("Found nothing for: %r", s)
        return []

    def _fuzzyFind(self, search):
        """Try to find a matching location, or list of locations, by name or alias.
        
        Args:
            search (str): the search string
        
        Returns:
            (list) a list of (score, location) tuples.
        """
        ratio_cutoff = 0.4
        number_cutoff = 5

        nonalnum = lambda x: not x.isalnum()
        matches = []
        for loc in self.locations['ByID'].values():
            if hasattr(loc, 'aliases'):
                aliases = loc['aliases']
            else:
                aliases = []
            for s in [loc['name']] + aliases:
                ratio = difflib.SequenceMatcher(nonalnum, search, s).ratio()
                if ratio >= ratio_cutoff:
                    matches.append((ratio, loc))
        matches.sort(lambda x, y: cmp(y[0], x[0]))
        return matches[:number_cutoff]

    def pickScale(self, path_info):
        """Picks zoom level based on a PathInfo object.

        Args:
            path_info (PathInfo)

        Returns:
            (int) a good zoom level
        """
        if path_info is None:
            return self.default_scale

        # Zoom outwards, checking if each rect will fit in the viewport
        for i in xrange(len(self.scales)):
            if (path_info.w*self.scales[i] > self.viewport_w - 5
                    or path_info.h*self.scales[i] > self.viewport_h - 5):
                continue
            else:
                return i
        # Return the largest scale if we failed to find a suitable one.
        return len(self.scales) - 1


    def pickOffsets(self, src, dst, path_info):
        """Picks x and y offsets for the viewport.

        Any of the arguments can be None. PathInfo is preferred.

        Args:
            src: (location hash)
            dst: (location hash)
            path_info: (PathInfo)

        Returns:
            (tuple(int, int)) good x and y offsets
        """
        if path_info is not None:
            return (path_info.x + path_info.w/2, path_info.y + path_info.h/2)
        # We have only one location
        elif src is not None:
            return (src['x'], src['y'])
        elif dst is not None:
            return (dst['x'], dst['y'])
        else:
            return (self.default_xoff, self.default_yoff)


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
