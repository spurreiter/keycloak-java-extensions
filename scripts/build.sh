#!/usr/bin/env bash

cwd=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

cd "$cwd/.."

files=(
  "$cwd/../request-header-oidc-mapper/target/request-header-oidc-mapper*"
  "$cwd/../mfa-auth/target/mfa-auth*"
  # "$cwd/../extensions-ear/target/extensions-ear*"
)

_mvn(){
  # mvn clean install 
  mvn clean install -DskipTests
}

_clean(){
  local pwd=$(pwd) 
  cd "$cwd/../deployments"
  rm $(ls | egrep -i '\.[ej]ar$')
  cd "$pwd"
}

_deploy(){
  for file in ${files[*]}; do
    echo "$file"
    cp "$file" $cwd/../deployments
  done
}

_clean
_mvn
_deploy