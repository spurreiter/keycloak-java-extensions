#!/usr/bin/env bash

json=$1

if [ -z $json ]; then
  echo json file needed
fi

jq --sort-keys -r < ${json} > ${json}.1
if [ $? -eq 0 ]; then
  mv ${json}.1 $json
fi