#!/bin/bash
echo "Selected main class: $1"
if [ -n "$1" ];
then
	mvn -e exec:java -Dexec.mainClass="main.$1"
else
	echo "Must provide an executable MAIN class."
fi