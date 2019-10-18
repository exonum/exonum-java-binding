#!/usr/bin/env bash
# Generates python file from core/runtime.proto file.
# This file must only be run on demand (when DeployArguments message is changed).

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

PROTO_SOURCE_DIR=./../../../core/src/main/proto
PROTO_SOURCE=$PROTO_SOURCE_DIR/service_runtime.proto

if [[ ! -f ${PROTO_SOURCE} ]]; then
    echo "File $PROTO_SOURCE not found"
    exit 1
fi

protoc --proto_path=${PROTO_SOURCE_DIR} --python_out=. ${PROTO_SOURCE}
