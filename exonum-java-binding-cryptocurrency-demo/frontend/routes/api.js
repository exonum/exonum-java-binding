var express = require('express');
var request = require('request');
var router = express.Router();

router.get('/*', function (req, res, next) {
  var query = req.params[0];

  if (query.indexOf("explorer") == -1) {
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
  } else {
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
  }


});

router.post('/*', function (req, res, next) {
  var query = req.params[0];
  if (query.indexOf("explorer") > -1) {
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