#!/usr/bin/env bash

cwd=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

docker run -d \
  --name kc \
  -p 8080:8080 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -e KEYCLOAK_LOGLEVEL=DEBUG \
  -v ${cwd}/my-realm.json:/tmp/my-realm.json \
  -v ${cwd}/../deployments:/opt/jboss/keycloak/standalone/deployments \
  -e KEYCLOAK_IMPORT=/tmp/my-realm.json \
  jboss/keycloak:11.0.3

sleep 15

$cwd/setcredentials.sh
