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


class ViewHandler(webapp.RequestHandler):
    def get(self):
        self.display()

    def post(self):
        self.display()

    def display(self):
        default_xoff  = 4608
        default_yoff  = 3172
        # is the difference between these?
        default_scale = 3
        default_size = 1
        default_mpm = 20

        html_base = '/static'
        html_dir = html_base
        css_dir = html_base + '/css'
        img_dir = html_base + '/img'
        js_dir = html_base + '/js'

        # # subdirectories of $HTML base for specific types of data
        # CSS_DIR         = $HTML_BASE . '/css'
        # STATIC_IMG_DIR  = $HTML_BASE . '/images'
        # DYNAMIC_IMG_DIR = $HTML_BASE . '/dynamic'
        # DYNAMIC_IMG_PATH        = $HTML_PATH . '/dynamic'
        # PATH_IMG_DIR    = $DYNAMIC_IMG_DIR . '/paths'
        # PATH_IMG_PATH   = $DYNAMIC_IMG_PATH . '/paths'
        # GRID_IMG_DIR    = $STATIC_IMG_DIR . '/grid'
        #
        # # where binary input files are stored
        # DATA_DIR                = 'data'
        # # locations of the binary files that contain the graph of paths
        # POINT_FILE              = $DATA_DIR . '/binPointData.dat'
        # LOCATION_FILE   = $DATA_DIR . '/binLocationData.dat'
        # EDGE_FILE               = $DATA_DIR . '/binEdgeData.dat'
        template_values = {
            'html_dir': html_dir,
            'css_dir': css_dir,
            'img_dir': img_dir,
            'js_dir': js_dir,

            'self': '/map',

            'size': self.request.get("size") or default_size,
            'mpm': self.request.get("mpm") or default_mpm,

            'txt_src': self.request.get('from'),
            'txt_dst': self.request.get('to'),
        }
        path = os.path.join(os.path.dirname(__file__), 'js_tmpl.html')
        self.response.out.write(template.render(path, template_values))

