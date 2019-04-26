#!/bin/bash
slow_flag=false

print_usage() {
  printf "Usage: -s for slow game. Nothing for normal."
}

while getopts ':s' flag; do
  case "${flag}" in
    s) slow_flag=true ;;
    *) print_usage
       exit 1 ;;
  esac
done

if [ "$slow_flag" = true ] ; then
  google-chrome --new-window http://localhost:8000/slow.html --incognito --window-size=1440,1000
  printf "Launching slow version of the game.\n"
else
  google-chrome --new-window http://localhost:8000/index.html --incognito --window-size=1440,1000
fi
