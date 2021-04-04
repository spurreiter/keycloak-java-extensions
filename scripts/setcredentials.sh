#!/usr/bin/env bash

set -x

realm=my

_exec(){
  docker exec -it kc-ext $@
}
_kcadm(){
  _exec /opt/jboss/keycloak/bin/kcadm.sh $@
}
_auth(){
  _kcadm config credentials --server http://localhost:8080/auth --realm master --user admin --password admin
}
# _createRealm(){
#   _kcadm create realms -s realm=$realm -s enabled=true
# }
_createUser(){
  _kcadm create users -r $realm -s username="$1" -s enabled=true $3
  _setPassword $1 $2
}
_setPassword(){
  _kcadm set-password -r $realm --username "$1" --new-password "$2"
}
_setClientPassword(){
  id=$(_kcadm get clients -r my -q "clientId=$1" --fields id | jq -r .[0].id)
  _kcadm update -r $realm "clients/$id" -s "secret=$2"
}

# _exec sh
if [ $1 -gt 1 ]; then
  sleep $1 
fi
_auth
_createUser alice alice "-s firstName=Alice -s lastName=Anders -s email=alice@local.my -s emailVerified=true -s attributes.otpauth=true -s attributes.phoneNumberVerified=false -s attributes.phoneNumber=\"+1800800800\"" 
_createUser bob bob
_setClientPassword my-client d0b8122f-8dfb-46b7-b68a-f5cc4e25d000
