#!/bin/bash
# -----------------------------------------------------------------
# pathinfo2pathimage.sh -- Convert a pathinfo file to a pathimage file (sans
# actual image references). The actual images are assumed to live out on disk
# somewhere. This is just for the datastructure-munging.
# -----------------------------------------------------------------

IN=pathinfo.csv
OUT=pathimage.csv

echo 'id0,id1,zoom' > "$OUT"
tail -n +2 "$IN" | perl -p -e '@l = split(/,/); $_ = "$l[0],$l[1],0\n$l[0],$l[1],1\n$l[0],$l[1],2\n$l[0],$l[1],3\n";' >> "$OUT"
