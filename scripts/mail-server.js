#!/usr/bin/env node

/**
 * mock mail server
 */

const { simpleParser } = require('mailparser')
const { SMTPServer } = require('smtp-server')

const debug = require('debug')

/**
 * @typedef {object} Log
 * @property {Function} error
 * @property {Function} warn
 * @property {Function} info
 * @property {Function} debug
 */

/**
 * logger with levels
 * @type {Log} log
 */
// @ts-ignore
const log = (function log () {
  const MAXLEVEL = 'WARN'
  let isMaxLevel = false
  const levels = ['ERROR', 'WARN', 'INFO', 'DEBUG']
  levels.forEach(level => {
    const lc = level.toLowerCase()
    log[lc] = !isMaxLevel
      ? debug(`@my/paperbox:${level}`)
      : () => {}
    isMaxLevel = isMaxLevel || level === MAXLEVEL
  })
  return log
})()

function authorizeUser (auth, session, callback) {
  let err = null
  let result = null
  // @ts-ignore
  if (auth.username in this.options.users) {
    // @ts-ignore
    const { uid, password } = this.options.users[auth.username]
    if (auth.method === 'CRAM-MD5') {
      if (auth.validatePassword(password)) {
        result = { user: uid }
      }
    } else if (auth.method === 'PLAIN') {
      if (password === auth.password) {
        result = { user: uid }
      }
    } else if (auth.method === 'LOGIN') {
      if (password === auth.password) {
        result = { user: uid }
      }
    }
  } else {
    result = { user: 65536 } // nobody
  }
  if (!result) {
    err = new Error('Invalid username or password')
  }
  callback(err, result)
}

function Paperbox (options) {
  this.options = {
    smtpPort: 1025,
    users: {
      guest: { password: 'password', uid: 65535 },
      nobody: { password: null, uid: 65536 }
    }
  }
  Object.assign(this.options, options)
}

Paperbox.prototype.listen = function () {
  const smtpOptions = {
    banner: 'mail server for testing',
    authOptional: true,
    maxAllowedUnauthenticatedCommands: 100,
    logger: log,
    authMethods: ['CRAM-MD5', 'PLAIN', 'LOGIN'],
    onData: processMailData.bind(this),
    onAuth: authorizeUser.bind(this),
    onRcptTo: validateRcptTo.bind(this),
    disabledCommands: ['STARTTLS']
  }
  const smtpServer = new SMTPServer(smtpOptions)
  smtpServer.listen(this.options.smtpPort)
  console.info('mail server started on port %s', this.options.smtpPort)
}

Paperbox.prototype.addMTAFields = function (mail) {
  const now = new Date()
  mail.received = now
}

Paperbox.prototype.onMailSaved = function (err, mailId) {
  if (err) {
    log.error('ERROR: failed saving mail:', err)
  } else {
    log.debug('mail saved, id:', mailId)
  }
}

async function processMailData (stream, session, callback) {
  log.debug('processMailData() - start')
  log.debug('  session = ', session)
  const mboxes = session.envelope.rcptTo.map(function (x) { return x.address })
  const parsed = await simpleParser(stream)
  // @ts-ignore
  this.addMTAFields(parsed)
  for (let i = 0; i < mboxes.length; i++) {
    log.debug('save mail to [' + mboxes[i] + ']')
    const { text, to, from } = parsed
    console.log(mboxes[i], { text, to, from })
  }
  callback()
  log.debug('processMailData() - done')
}

function validateRcptTo (address, session, callback) {
  log.debug(`validateRcptTo(${address.address}, ${address.args}) ... OK`)
  callback()
}

const server = new Paperbox()
server.listen()
