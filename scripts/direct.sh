#!/bin/bash

# script to login with direct grant

curl -v \
  -H "X-Request-Id: e140abbf-4bf1-4e2e-b579-fa1f81b8ab08" \
  -d "client_id=my-server" \
  -d "grant_type=password" \
  -d "scope=openid" \
  --data-urlencode "username=$1" \
  --data-urlencode "password=$2" \
  --data-urlencode "otp=$3" \
  http://localhost:8080/auth/realms/my/protocol/openid-connect/token \


#| jwtdecode
