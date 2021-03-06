const supertest = require('supertest')
const express = require('express')
const log = require('debug')('test')

const CONFIG = {
  keycloak: {
    url: 'http://localhost:8080'
  }
}

/**
 * if fails start docker container with
 * `./scripts/docker-run.sh`
 * then add users with
 * `./scripts/setcredentials.sh`
 */
const testConnectKeycloak = () => supertest(CONFIG.keycloak.url).get('/auth').expect(303)

const mfaMockServer = (scenario) => {
  const app = express()
  let server

  // logger and body-parser - is unsafe
  app.use((req, res, next) => {
    let text = ''
    req.on('data', chunk => { text += chunk.toString() })
    req.on('end', () => {
      const { method, url } = req
      req.body = JSON.parse(text)
      log({ method, url, text })
      next()
    })
  })
  app.post('/mfa', (req, res) => {
    const { email: destination, nonce } = req.body
    const expiresAt = Date.now() + 300000
    let status = 200
    let body = { destination, expiresAt, nonce }
    if (!destination) {
      status = 400
      body = { status, error: 'missing_id' }
    }
    res.status(status).json(body)
  })
  app.put('/mfa/verify', (req, res) => {
    const { code, nonce } = req.body
    let status = 200
    let body = { nonce }
    if (code !== '123456') {
      status = 403
      body = { status, error: 'mfa_invalid' }
    }
    res.status(status).json(body)
  })
  app.start = ({ port = 1080, done }) => {
    server = app.listen(port, '127.0.0.1', done)
    return app
  }
  app.stop = () => {
    server && server.close()
  }

  return app
}

module.exports = {
  CONFIG,
  testConnectKeycloak,
  mfaMockServer
}
