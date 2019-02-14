/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
app.use('/api/explorer', proxy({ target: explorerRoot, changeOrigin: true }))
app.use('/api', proxy({ target: apiRoot, changeOrigin: true }))

// Single Page Application entry point
app.get('/', function (req, res) {
  res.sendFile('index.html')
})

app.listen(port)
