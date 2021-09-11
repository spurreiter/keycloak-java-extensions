# keycloak java extensions

Available extension(s):

## request-header-oidc-mapper

[Map request headers into the Oidc Token](./request-header-oidc-mapper/docs/README.md)

## mfa-auth

[Multi-factor-authentication using external service](./mfa-auth/docs/README.md)

# start server

The server starts with a sample realm `my` with all extensions configured.

```sh
# start server
./scripts/docker-run.sh
# deploy ear 
./scripts/build.sh
# add users if server is up
./scripts/create-users.js
# start the test server for OTP
npm start
```

## development

build extensions
```sh
./scripts/build.sh
```

run integration tests (uses nodejs) 
```sh
# install
npm i 
# run tests
npm t
```

set version
```sh
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
```

## license

Apache 2.0
