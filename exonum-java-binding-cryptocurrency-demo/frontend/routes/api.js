const express = require('express');
const request = require('request');
const router = express.Router();

router.get('/*', function (req, res, next) {
  const query = req.params[0];
  if (~query.indexOf("cryptocurrency-demo-service")) {
    request.get({
      url: req.app.get('apiRoot') + '/api/' + query,
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
  } else {
    request.get({
      url: req.app.get('explorerRoot') + '/api/' + query,
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
    
  }
});

router.post('/*', function (req, res, next) {
  const query = req.params[0];
  if (~query.indexOf("cryptocurrency-demo-service")) {
    request.post({
        url: req.app.get('apiRoot') + '/api/' + query,
        json: req.body
      },
      function (err, response, body) {
        if (err) {
          return next(err);
        }
        res.json(body);
      });
  } else {
    request.post({
        url: req.app.get('explorerRoot') + '/api/' + query,
        json: req.body
      },
      function (err, response, body) {
        if (err) {
          return next(err);
        }
        res.json(body);
      });
  }

});

module.exports = router;
