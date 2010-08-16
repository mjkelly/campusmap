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
import logging

import campusmap

class ViewHandler(webapp.RequestHandler):
    def get(self):
        self.display()

    def post(self):
        self.display()

    def display(self):
        # this is the core map logic
        m = campusmap.Map()

        src = self.request.get("from")
        dst = self.request.get("to")

        src_loc = m.findLocation(src)
        dst_loc = m.findLocation(dst)

        template_values = {
            'html_dir': m.html_base,
            'css_dir': m.html_base + '/css',
            'img_dir': m.html_base + '/img',
            'js_dir': m.html_base + '/js',
            'grid_dir': m.html_base + '/tiles',
            'paths_dir': '/p',

            'self': '/map',

            'size': self.request.get("size") or m.default_size,
            'mpm': self.request.get("mpm") or m.default_mpm,

            'txt_src': src,
            'txt_dst': dst,

            # TODO: need to calculate these
            'txt_src_official' : '',
            'txt_dst_official' : '',

            # TODO: make these persist properly across pageloads
            'xoff' : m.default_xoff,
            'yoff' : m.default_yoff,

            'width': 500,
            'height': 375,
            'scale': m.default_scale,
            # TODO: should be totally deprecated
            'map_name': 'visitor',
        
            'location_opt': m.locations_menu,
        }

        if len(src_loc) == 1:
            template_values['src_found'] = '1'
            template_values['txt_src_official'] = src_loc[0]['name']
            template_values['src_name'] = src_loc[0]['name']
            template_values['src_x'] = src_loc[0]['x']
            template_values['src_y'] = src_loc[0]['y']
        elif len(src_loc) > 1:
            template_values['src_help'] = '<p>Multiple src locs found!</p>'

        if len(dst_loc) == 1:
            template_values['dst_found'] = '1'
            template_values['txt_dst_official'] = dst_loc[0]['name']
            template_values['dst_name'] = dst_loc[0]['name']
            template_values['dst_x'] = dst_loc[0]['x']
            template_values['dst_y'] = dst_loc[0]['y']
        elif len(dst_loc) > 1:
            template_values['dst_help'] = '<p>Multiple dst locs found!</p>'

        if len(src_loc) == 1 and len(dst_loc) == 1:
            path_info = campusmap.PathInfo.fromSrcDst(src_loc[0]['id'], dst_loc[0]['id'])
            if path_info:
                logging.info("Got PathInfo: %s", path_info)
                template_values['path_found'] = True;
                template_values['path_x'] = path_info.x
                template_values['path_y'] = path_info.y
                template_values['path_w'] = path_info.w
                template_values['path_h'] = path_info.h
                template_values['path_dist'] = path_info.dist
                template_values['path_src'] = src_loc[0]['id']
                template_values['path_dst'] = dst_loc[0]['id']
            else:
                logging.error("Couldn't get PathInfo for IDs: %s %s", src_loc[0]['id'], dst_loc[0]['id'])
                template_values['txt_error'] = "Internal error: couldn't retrieve path between locations!"

        path = os.path.join(os.path.dirname(__file__), m.main_tmpl)
        self.response.out.write(template.render(path, template_values))

