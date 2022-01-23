const supertest = require('supertest')
const express = require('express')
const log = require('debug')('test:mfa')

const CODE = '000000'

/**
 * if fails start docker container with
 * `./scripts/docker-run.sh`
 */
const testConnectKeycloak = (config) => supertest(config.keycloak.url).get('/auth').expect(303)

const mfaMockServer = () => {
  const app = express()

  console.log(`Answer with OTP-Code "${CODE}"`)

  // logger and body-parser - is unsafe
  app.use((req, res, next) => {
    let text = ''
    req.on('data', chunk => { text += chunk.toString() })
    req.on('end', () => {
      const { method, url } = req
      try {
        req.body = JSON.parse(text)
      } catch (e) {
        req.body = {}
      }
      log({ method, url, text })
      next()
    })
  })
  app.post('/mfa', (req, res) => {
    const { email, phoneNumber, nonce } = req.body
    const destination = phoneNumber || email
    const expiresAt = Date.now() + 300000
    let status = 200
    let body = { destination, expiresAt, nonce }
    if (!destination) {
      status = 400
      body = { status, error: 'missing_id' }
    }
    res.status(status).json(body)
  })
  app.put('/mfa', (req, res) => {
    const { code, nonce } = req.body
    let status = 200
    let body = { nonce }
    if (code !== CODE) {
      status = 403
      body = { status, error: 'mfa_invalid' }
    }
    res.status(status).json(body)
  })
  app.post('/mfa/send-email', (req, res) => {
    const { link, email, nonce, requestId } = req.body
    let status = 200
    let body = { nonce }
    if (!link || !email) {
      status = 500
      body = {
        requestId,
        error: 'Missing link or email',
        status
      }
    }
    res.status(status).json(body)
  })
  app.start = ({ host = 'localhost', port = 1080, done }) => {
    app._server = app.listen(port, host, done)
    return app
  }
  app.stop = () => {
    app._server && app._server.close()
  }

  return app
}

module.exports = {
  testConnectKeycloak,
  mfaMockServer
}
