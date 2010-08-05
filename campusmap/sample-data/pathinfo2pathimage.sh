#!/bin/bash
# -----------------------------------------------------------------
# pathinfo2pathimage.sh -- Convert a pathinfo file to a pathimage file (sans
# actual image references). The actual images are assumed to live out on disk
# somewhere. This is just for the datastructure-munging.
# -----------------------------------------------------------------

tail -n +2 pathinfo.csv | perl -p -e 'chomp; $_ = "$_,0\n$_,1\n$_,2\n$_,3\n";' > pathimage.csv
