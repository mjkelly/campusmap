#!/bin/bash
# -----------------------------------------------------------------
# clearcache.sh -- $desc$
# Copyright 2005 Michael Kelly (jedimike.net)
#
# This program is released under the terms of the GNU General Public
# License as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# Thu Sep  8 23:07:03 PDT 2005
# -----------------------------------------------------------------
cache_suffix='.cache'
img_suffix='.png'

cache=~/www/cgi-bin/ucsdmap/cache
path=~/www/ucsdmap/dynamic/paths
dyn=~/www/ucsdmap/dynamic

echo "From ${cache}:"
ls -l ${cache}/*${cache_suffix}
rm -f ${cache}/*${cache_suffix}
echo "From ${path}:"
ls -l ${path}/*${img_suffix}
rm -f ${path}/*${img_suffix}
echo "From ${dyn}:"
ls -l ${dyn}/*${img_suffix}
rm -f ${dyn}/*${img_suffix}
