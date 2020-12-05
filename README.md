# keycloak java extensions

Available extension(s)

## request-header-oidc-mapper

[Map request headers into the Oidc Token](./docs/request-header-oidc-mapper/README.md)

## start server

```sh
./scripts/docker-run.sh
```

server starts with sample realm `my`.

## development

build extensions
```sh
mvn clean install
```

set version
```sh
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
```

## license

Apache 2.0
