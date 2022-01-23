#!/usr/bin/env node

/**
 * script to setup realm
 */

const KcAdmin = require('@keycloak/keycloak-admin-client').default

const log = console.log

const config = {
  baseUrl: 'http://localhost:8080/auth',
  realm: 'my',
  username: 'admin',
  password: 'admin',
  email: 'admin@my.local'
}

const adminUser = {
  username: config.username,
  emailVerified: true,
  email: 'admin@my.local',
  firstName: 'Super',
  lastName: 'Admin'
}

const users = [{
  username: 'alice',
  enabled: true,
  totp: false,
  emailVerified: true,
  firstName: 'Alice',
  lastName: 'Anders',
  email: 'alice@my.local',
  attributes: {
    phoneNumber: '+10000001',
    phoneNumberVerified: false,
    otpauth: true
  }
}, {
  username: 'bob',
  enabled: true,
  totp: false,
  emailVerified: true,
  firstName: 'Bob',
  lastName: 'Builder',
  // email: 'bob@my.local',
  attributes: {
    phoneNumber: '+10000002',
    phoneNumberVerified: false
  }
}]

// authenticate as admin user
async function client (config) {
  const { username, password, baseUrl } = config
  const kc = new KcAdmin({
    baseUrl,
    realmName: 'master'
  })
  await kc.auth({
    username,
    password,
    grantType: 'password',
    clientId: 'admin-cli'
  })
  return kc
}

// set admin attributes
async function setAdmin (kc, adminUser) {
  const { username } = adminUser
  const users = await kc.users.find({ realm: 'master', username })
  // console.log(users)
  const id = users[0].id

  await kc.users.update(
    { id },
    adminUser
  )
  log('INFO: admin user updated')
}

// import local users
async function importUsers (kc, users, { realm }) {
  const currentUsers = await kc.users.find({ realm })
  const importUsernames = users.map(user => user.username)
  for (const user of currentUsers) {
    if (importUsernames.includes(user.username)) {
      // console.log(user)
      await kc.users.del({ realm, id: user.id })
    }
  }

  for (const user of users) {
    const { id } = await kc.users.create({ ...user, realm })
    await kc.users.resetPassword({
      id,
      realm,
      credential: {
        temporary: false,
        type: 'password',
        value: user.username // password is same as username
      }
    })
    log(`INFO: user "${user.username}" password "${user.username}" created`)
  }
}

async function main () {
  const { realm } = config
  const kc = await client(config)
  await setAdmin(kc, adminUser)
  await importUsers(kc, users, { realm })
}

module.exports = main

if (module === require.main) {
  main().catch(console.error)
}
