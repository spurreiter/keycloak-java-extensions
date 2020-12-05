#!/bin/bash

cwd=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

# sample request for request-header-oidc-mapper

curl -X POST -v \
  -H "x-some-id: 57a5cb16-1344-470f-8168-24666af9605e" \
  -H "x-tenant-id: 7847c809-c441-4179-b86b-160417961d9e" \
  -d "client_id=my-client" \
  -d "client_secret=d0b8122f-8dfb-46b7-b68a-f5cc4e25d000" \
  -d "grant_type=client_credentials" \
  -d "scope=openid" \
  http://localhost:8080/auth/realms/my/protocol/openid-connect/token
