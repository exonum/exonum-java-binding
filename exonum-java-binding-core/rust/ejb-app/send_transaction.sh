#!/usr/bin/env bash

set -eu -o pipefail

curl -d '{"service_id":"42", "message_id":"1", "name": "Test" }' -H "Content-Type: application/json" \
    -X POST http://localhost:6000/cryptocurrency-demo-service/submit-transaction