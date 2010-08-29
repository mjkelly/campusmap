#!/bin/bash
# -----------------------------------------------------------------
# pathinfo2pathimage.sh -- Convert a pathinfo file to a pathimage file (sans
# actual image references). The actual images are assumed to live out on disk
# somewhere. This is just for the datastructure-munging.
# -----------------------------------------------------------------

IN=pathinfo.csv
OUT=pathimage.csv

echo 'id0,id1,zoom' > "$OUT"
tail -n +2 "$IN" | perl -p -e '@l = split(/,/); $_ = "$l[1],$l[2],0\n$l[1],$l[2],1\n$l[1],$l[2],2\n$l[1],$l[2],3\n";' >> "$OUT"
