#!/usr/bin/env bash

set -x

cwd=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
os=$(uname)

if [ $os = "Darwin" ]; then
  sed -i .bak "s#http://localhost:1080#http://host.docker.internal:1080#g" "$cwd/my-realm.json"
else 
  sed -i .bak "s#http://host.docker.internal:1080#http://localhost:1080#g" "$cwd/my-realm.json"
fi

setcredentials() {
  "$cwd/setcredentials.sh" 30
}

# setcredentials &

docker run \
  -it --rm \
  --name kc-ext \
  -p 8080:8080 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -e KEYCLOAK_LOGLEVEL=DEBUG \
  -v ${cwd}/my-realm.json:/tmp/my-realm.json \
  -v ${cwd}/../deployments:/opt/jboss/keycloak/standalone/deployments \
  -v ${cwd}/configuration:/opt/jboss/keycloak/standalone/configuration \
  -e KEYCLOAK_IMPORT=/tmp/my-realm.json \
  jboss/keycloak:12.0.4

