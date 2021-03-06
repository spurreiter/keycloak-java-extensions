# keycloak java extensions

Available extension(s):

## request-header-oidc-mapper

[Map request headers into the Oidc Token](./docs/request-header-oidc-mapper/README.md)

## mfa-auth

[Multi-factor-authentication using external service](./docs/mfa-auth/README.md)

# start server

The server starts with a sample realm `my` with all extensions configured.

```sh
# start server
./scripts/docker-run.sh
# deploy ear 
./scripts/deploy.sh
# add users if server is up
./scripts/setcredentials.sh
```

## development

build extensions
```sh
./scripts/deploy.sh
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
