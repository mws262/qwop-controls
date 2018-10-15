#!/bin/bash
echo "Selected goals class: $1"
if [ -n "$1" ];
then
	mvn -e exec:java -Dexec.mainClass="$1"
else
	echo "Must provide an executable MAIN class."
fi
