#!/usr/bin/env bash

# prints a section header
function header() {
    local title=$1
    local rest="========================================================================"
    echo
    echo "===[ ${title} ]${rest:${#title}}"
    echo
}

set -eu -o pipefail

header "DETECTING ENVIRONMENT"

# Check whether the Exonum Java App is installed.
EXONUM_JAVA_APP="exonum-java"
command -v ${EXONUM_JAVA_APP} >/dev/null 2>&1 || { echo >&2 "Please install the Exonum Java App and make sure that 'exonum-java' binary is available via PATH. Aborting."; exit 1; }

# Find the latest version of artifact and build if not found.
ARTIFACT_PATH="$(find ./target -type f -name exonum-java-binding-cryptocurrency-demo-*-artifact.jar)"

if [ -z "${ARTIFACT_PATH}" ];
then
    mvn package
fi
ARTIFACT_PATH="target"
echo "ARTIFACT_PATH=${ARTIFACT_PATH}"

# Clear test dir.
rm -rf testnet
mkdir testnet

# Enable predefined native logging configuration,
# unless it is already set to any value (incl. null)
export RUST_LOG="${RUST_LOG-error,exonum=info,exonum-java=info,java_bindings=info}"

header "GENERATE COMMON CONFIG"
${EXONUM_JAVA_APP} generate-template \
    --validators-count=1 \
    testnet/common.toml

header "GENERATE CONFIG"
${EXONUM_JAVA_APP} generate-config \
    testnet/common.toml \
    testnet \
    --no-password \
    --peer-address 127.0.0.1:5400

header "FINALIZE"
${EXONUM_JAVA_APP} finalize \
    testnet/sec.toml \
    testnet/node.toml \
    --public-configs testnet/pub.toml

header "START TESTNET"
${EXONUM_JAVA_APP} run \
    --node-config testnet/node.toml \
    --artifacts-path ${ARTIFACT_PATH} \
    --db-path testnet/db \
    --master-key-pass pass \
    --public-api-address 127.0.0.1:3000 \
    --private-api-address 127.0.0.1:3010 \
    --ejb-port 7000
