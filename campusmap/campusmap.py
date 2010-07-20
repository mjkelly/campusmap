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
import os
import urllib

class Map:
    _locations_html = 'locations.html'

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
        self.locations = fh.read()
        fh.close()

    def isKeyword(self, s):
        """Return if s is a location keyword.

        E.g., closest food, closest parking, etc.
        """
        return s.lower().startswith('keyword:')
        
