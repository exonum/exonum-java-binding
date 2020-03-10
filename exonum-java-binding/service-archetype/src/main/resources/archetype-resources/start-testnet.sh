#!/usr/bin/env bash

# Starts a test Exonum network with a single validator node.
#
# The network files are placed in `$PWD/testnet` directory, which is cleared at the beginning.
# The Java service artifacts must be placed into 'testnet/artifacts' directory.
#
# See https://exonum.com/doc/version/latest/get-started/java-binding/#node-configuration
# for extra information.

# Prints a section header
function header() {
    local title=$1
    local rest="========================================================================"
    echo
    echo "===[ ${title} ]${rest:${#title}}"
    echo
}

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Check whether the Exonum Java App is installed.
command -v exonum-java >/dev/null 2>&1 || {
  echo >&2 "Please install the Exonum Java App and make sure that 'exonum-java' binary is available via PATH:
  - Installation instructions: https://exonum.com/doc/version/latest/get-started/java-binding/#installation
  - PATH=${PATH}

Aborting.";
  exit 1;
}

# Clear test dir.
TESTNET_PATH="$PWD/testnet"
rm -rf "$TESTNET_PATH"
mkdir "$TESTNET_PATH"

# Enable predefined native logging configuration,
# unless it is already set to any value (incl. null)
export RUST_LOG="${RUST_LOG-error,exonum=info,exonum-java=info,java_bindings=info}"

header "1 Generate Template Config"
exonum-java generate-template \
    --validators-count=1 \
    --supervisor-mode simple \
    "$TESTNET_PATH"/common.toml

header "2 Generate Node Private and Public Configs"
exonum-java generate-config \
    "$TESTNET_PATH"/common.toml \
    "$TESTNET_PATH" \
    --no-password \
    --peer-address 127.0.0.1:5400

header "3 Finalize Configuration"
exonum-java finalize \
    "$TESTNET_PATH"/sec.toml \
    "$TESTNET_PATH"/node.toml \
    --public-configs "$TESTNET_PATH"/pub.toml

header "4 Start Test Network"
exonum-java run \
    --node-config "$TESTNET_PATH"/node.toml \
    --artifacts-path "$TESTNET_PATH"/artifacts \
    --db-path "$TESTNET_PATH"/db \
    --master-key-pass pass \
    --public-api-address 127.0.0.1:3000 \
    --private-api-address 127.0.0.1:3010 \
    --ejb-port 7000
