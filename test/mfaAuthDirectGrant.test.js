const assert = require('assert')
const supertest = require('supertest')
const createUsers = require('../scripts/create-users.js')
const config = require('./config.js')
const { testConnectKeycloak, mfaMockServer } = require('./support.js')

describe('mfa-auth direct grant', function () {
  const username = 'alice'
  const password = 'alice'
  const clientId = 'my-server'

  before(async function () {
    await createUsers()
  })

  before(function () {
    // console.log(config)
    return testConnectKeycloak(config)
  })

  before(function (done) {
    this.app = mfaMockServer().start({ ...config.rest, done })
  })
  after(function () {
    this.app.stop()
  })

  it('shall start otp sending', function () {
    return supertest(config.keycloak.url)
      .post('/auth/realms/my/protocol/openid-connect/token')
      .set({
        'X-Request-ID': 'e140abbf-4bf1-4e2e-b579-fa1f81b8ab08'
      })
      .type('form')
      .send({
        client_id: clientId,
        grant_type: 'password',
        scope: 'openid',
        username,
        password
      })
      .expect(401, {
        error: 'mfa_sent',
        error_description: 'Authentication code sent.'
      })
  })

  it('shall fail on wrong otp value', function () {
    return supertest(config.keycloak.url)
      .post('/auth/realms/my/protocol/openid-connect/token')
      .set({
        'X-Request-ID': 'e140abbf-4bf1-4e2e-b579-fa1f81b8ab08'
      })
      .type('form')
      .send({
        client_id: clientId,
        grant_type: 'password',
        scope: 'openid',
        otp: '100000',
        username,
        password
      })
      .expect(401, {
        error: 'mfa_invalid',
        error_description: 'Invalid code, please try again.'
      })
  })

  it('shall succeed on valid otp value', function () {
    return supertest(config.keycloak.url)
      .post('/auth/realms/my/protocol/openid-connect/token')
      .set({
        'X-Request-ID': 'e140abbf-4bf1-4e2e-b579-fa1f81b8ab08'
      })
      .type('form')
      .send({
        client_id: clientId,
        grant_type: 'password',
        scope: 'openid',
        otp: '000000',
        username,
        password
      })
      .expect(200)
      .then(({ body }) => {
        assert.strictEqual(typeof body.access_token, 'string')
        assert.strictEqual(body.token_type, 'Bearer')
      })
  })
})
