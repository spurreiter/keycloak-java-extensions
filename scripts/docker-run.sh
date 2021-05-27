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
  -e KEYCLOAK_IMPORT=/tmp/my-realm.json \
  jboss/keycloak:13.0.1

#  -v ${cwd}/configuration-13.0.1:/opt/jboss/keycloak/standalone/configuration \

###
# for templating unset cache:
#
# docker cp kc-ext:/opt/jboss/keycloak/standalone/configuration .
#
# scripts/configuration-13.0.1/standalone-ha.xml
#   <subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">
#     ...
#     <theme>
#         <!--
#         <staticMaxAge>2592000</staticMaxAge>
#         <cacheThemes>true</cacheThemes>
#         <cacheTemplates>true</cacheTemplates>
#         -->
#         <staticMaxAge>-1</staticMaxAge>
#         <cacheThemes>false</cacheThemes>
#         <cacheTemplates>false</cacheTemplates>
#         <welcomeTheme>${env.KEYCLOAK_WELCOME_THEME:keycloak}</welcomeTheme>
#         <default>${env.KEYCLOAK_DEFAULT_THEME:keycloak}</default>
#         <dir>${jboss.home.dir}/themes</dir>
#     </theme>
# </subsystem>

