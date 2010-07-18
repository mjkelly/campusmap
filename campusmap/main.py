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
from google.appengine.ext.webapp import util

import map_handlers

class MainHandler(webapp.RequestHandler):
    def get(self):
        response = """
<html>
<head>
    <title>CampusMap</title>
</head>
<body>
<h1>CampusMap</h1>
<p>Foo.</p>
</body>
</html>
"""
        self.response.out.write(response)

def main():
    application = webapp.WSGIApplication([('/', MainHandler),
                                          ("/map/(.*)", map_handlers.ViewHandler)],
                                         debug=True)
    util.run_wsgi_app(application)


if __name__ == '__main__':
    main()
