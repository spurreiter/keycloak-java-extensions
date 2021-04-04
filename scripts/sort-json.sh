#!/usr/bin/env bash

in=$1
out=$2

if [ -z $1 ]; then
  echo input file needed
  exit 1
fi
if [ -z $2 ]; then
  echo output file needed
  exit 1
fi
if [ $1 = $2 ]; then
  echo input and output must not be same file
  exit 1
fi

jq --sort-keys -r < ${in} > ${out}
