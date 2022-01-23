#!/usr/bin/env bash

set -x

cwd=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

. "$cwd/env"

(cat << EOS
FROM $IMAGE

COPY scripts/my-realm.json /tmp/my-realm.json
COPY deployments /opt/jboss/keycloak/standalone/deployments

EOS
) > "$cwd/Dockerfile"

cd "$cwd/.."

DOCKER_BUILDKIT=0 docker build \
  --file "$cwd/Dockerfile" \
  --tag keycloak-ldap-ext:$VERSION \
  .

rm "$cwd/Dockerfile"