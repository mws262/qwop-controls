#!/bin/bash
mkdir ./eps/
for filename in ./*.drawio; do
    [ -e "$filename" ] || continue
    drawio -x --crop -f eps -o "./eps/$(basename "$filename" .drawio).eps" $filename 
done

