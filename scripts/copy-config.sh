#!/usr/bin/env bash

set -x

cwd=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

cd "$cwd/.."
test ! -d devresources && mkdir devresources

cd "$cwd/../devresources"

docker cp kc-ext:/opt/jboss/keycloak/standalone/configuration .
docker cp kc-ext:/opt/jboss/keycloak/themes .