const express = require('express');
const request = require('request');
const router = express.Router();

router.get('/*', function (req, res, next) {
  const query = req.params[0];
  const url = ~query.indexOf("cryptocurrency-demo-service") ? 
    req.app.get('apiRoot') + '/api/' + query :
    req.app.get('explorerRoot') + '/api/' + query
  request.get({
    url: url,
    qs: req.query
  }, function (err, response, body) {
    if (err) {
      return next(err);
    }
    try {
      res.json(JSON.parse(body));
    } catch (e) {
      res.json({});
    }
  });
});

router.post('/*', function (req, res, next) {
  const query = req.params[0];
  const url = ~query.indexOf("cryptocurrency-demo-service") ? 
    req.app.get('apiRoot') + '/api/' + query :
    req.app.get('explorerRoot') + '/api/' + query
  request.post({
    url: url,
    json: req.body
    }, function (err, response, body) {
      if (err) {
        return next(err);
      }
      res.json(body);
  });
});

module.exports = router;
