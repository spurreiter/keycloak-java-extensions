# mfa-auth

Multi-Factor-Authenticaton with generic REST endpoint.

## Overview 

![overview](./mfa-auth.drawio.png)

## Config

Browser

![](./mfa-browser-flow-1.png)

![](./mfa-browser-flow-2.png)

Direct-Grant

![](./mfa-direct-flow-1.png)

![](./mfa-direct-flow-2.png)

## API 

The REST Endpoint handles:
- OTP Code Generation 
- Sending the OTP back to the user
- Retries, Expiry and Validation

For documentation of the API and a sample implementation please check [keycloak-ldap][].

[keycloak-ldap]: https://github.com/spurreiter/keycloak-ldap/docs/mfa-api.md