const express = require('express');
const bodyParser = require('body-parser');

// Initialize application
const app = express();

// Get app params
const argv = require('yargs-parser')(process.argv.slice(2));
const port = argv.port;
const explorerRoot = argv.explorerRoot;
const apiRoot = argv.apiRoot;

if (typeof port === 'undefined') {
  throw new Error('--port parameter is not set.');
}

if (typeof apiRoot === 'undefined') {
  throw new Error('--api-root parameter is not set.');
}

if (typeof explorerRoot === 'undefined') {
  throw new Error('--explorer-root parameter is not set.');
}

app.set('apiRoot', apiRoot);
app.set('explorerRoot', explorerRoot);

// Configure parsers
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: true}));

// Set path to static files
app.use(express.static(__dirname + '/'));

// Activate routers
const api = require('./routes/api');
app.use('/api', api);

app.listen(port);
