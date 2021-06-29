const supertest = require('supertest')
const assert = require('assert')

const config = require('./config.js')
const { testConnectKeycloak } = require('./support.js')

describe('request-header-oidc-mapper', function () {
  before(function () {
    return testConnectKeycloak(config)
  })

  const clientSecret = 'e7138d77-d492-4d54-b71d-0ad92070059c'

  const xSomeId = '57a5cb16-1344-470f-8168-24666af9605e'
  const xTenantId = 'eb6d4a17-aaba-48b0-9a42-b5f43eeaa8d6'

  it('shall return access token for service account', function () {
    return supertest(config.keycloak.url)
      .post('/auth/realms/my/protocol/openid-connect/token')
      .set({
        'x-tenant-id': xTenantId,
        'x-some-id': xSomeId
      })
      .type('form')
      .send({
        client_id: 'my-client',
        client_secret: clientSecret,
        grant_type: 'client_credentials',
        scope: 'openid'
      })
      .expect(200)
      .then(({ body }) => {
        // console.log({ body })
        assert.strictEqual(typeof body.access_token, 'string')
      })
  })

  it('shall fail if header is missing', function () {
    return supertest(config.keycloak.url)
      .post('/auth/realms/my/protocol/openid-connect/token')
      .set({
        'x-tenant-id': xTenantId
      })
      .type('form')
      .send({
        client_id: 'my-client',
        client_secret: clientSecret,
        grant_type: 'client_credentials',
        scope: 'openid'
      })
      .expect(400, {
        error: 'invalid_request',
        error_description: 'Header missing'
      })
  })

  it('shall fail if secret is missing', function () {
    return supertest(config.keycloak.url)
      .post('/auth/realms/my/protocol/openid-connect/token')
      .set({
        'x-tenant-id': xTenantId,
        'x-some-id': xSomeId
      })
      .type('form')
      .send({
        client_id: 'my-client',
        client_secret: 'e0b8122f-8dfb-46b7-b68a-f5cc4e25d000',
        grant_type: 'client_credentials',
        scope: 'openid'
      })
      .expect(401, {
        error: 'unauthorized_client',
        error_description: 'Invalid client secret'
      })
  })
})
