
process.env.DEBUG = 'test*'

const config = require('./config.js')
const { mfaMockServer } = require('./support.js')
const { setup } = require('./client.js')

if (module === require.main) {
  const serverRest = mfaMockServer().start({
    ...config.rest,
    done: () => {
      console.log('Rest on http://%s:%s', serverRest.address().address, serverRest.address().port)
    }
  })._server

  const serverClient = setup().listen(config.client.port, config.client.host, () => {
    console.log('Client on http://%s:%s', serverClient.address().address, serverClient.address().port)
  })
}
