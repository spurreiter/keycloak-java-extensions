#!/usr/bin/env bash

set -x

cwd=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
os=$(uname)
opts=

. "$cwd/env"

if [ $os = "Darwin" ]; then
  echo
  #sed -i .bak "s#http://localhost:1080#http://host.docker.internal:1080#g" "$cwd/my-realm.json"
else 
  #sed -i.bak "s#http://localhost:1080#http://host.docker.internal:1080#g" "$cwd/my-realm.json"
  #sed -i.bak "s#http://host.docker.internal:1080#http://localhost:1080#g" "$cwd/my-realm.json"
  ## check with hostname -I
  opts="--add-host host.docker.internal:172.17.0.1 "
fi

setcredentials() {
  "$cwd/setcredentials.sh" 30
}

# setcredentials &

docker run \
  -it --rm $opts \
  --name kc-ext \
  -p 8080:8080 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -e KEYCLOAK_LOGLEVEL=INFO \
  -p 8787:8787 \
  -e DEBUG=true \
  -e DEBUG_PORT='*:8787' \
  -v ${cwd}/my-realm.json:/tmp/my-realm.json \
  -v ${cwd}/../deployments:/opt/jboss/keycloak/standalone/deployments \
  -v ${cwd}/disable-theme-cache.cli:/opt/jboss/startup-scripts/disable-theme-cache.cli \
  -e KEYCLOAK_IMPORT=/tmp/my-realm.json \
  $IMAGE

