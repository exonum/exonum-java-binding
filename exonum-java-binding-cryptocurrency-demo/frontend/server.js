var express = require('express')
var proxy = require('http-proxy-middleware')

// Initialize application
var app = express()

// Get app params
var argv = require('yargs-parser')(process.argv.slice(2))
var port = argv.port
var explorerRoot = argv.explorerRoot
var apiRoot = argv.apiRoot

if (typeof port === 'undefined') {
  throw new Error('--port parameter is not set.')
}

if (typeof apiRoot === 'undefined') {
  throw new Error('--api-root parameter is not set.')
}

if (typeof explorerRoot === 'undefined') {
  throw new Error('--explorer-root parameter is not set.')
}

// Set path to static files
app.use(express.static(__dirname + '/'))

// Activate proxy
app.use('/api', proxy({ target: apiRoot, changeOrigin: true }))

// Single Page Application entry point
app.get('/', function (req, res) {
  res.sendFile('index.html')
})

app.listen(port)
