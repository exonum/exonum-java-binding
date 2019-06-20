#!/usr/bin/env bash
# Runs exonum-java executable from target directory and starts one Exonum node with
# cryptocurrency service.
#
# This script is intended to be used by Exonum developers to speed up workflow.
# Running the executable from target doesn't need packaging and therefore can be performed
# immediately after running tests, without additional recompilation.

set -eu -o pipefail

# Fixes tha lack of the `realpath` tool in OS X.
if [ ! $(which realpath) ]; then
    function realpath() {
        python -c 'import os, sys; print os.path.realpath(sys.argv[1])' "${1%}"
    }
fi

# prints a section header
function header() {
    local title=$1
    local rest="========================================================================"
    echo
    echo "===[ ${title} ]${rest:${#title}}"
    echo
}

EJB_APP_DIR=$(pwd)
echo "CURRENT_DIR=${EJB_APP_DIR}"

EJB_ROOT=$(realpath "../../..")
echo "PROJ_ROOT=${EJB_ROOT}"

source "${EJB_ROOT}/tests_profile"

# Find the artifact
ARTIFACT_PATH="$(find ${EJB_ROOT} -type f -name exonum-java-binding-cryptocurrency-demo-*-artifact.jar)"
echo "ARTIFACT_PATH=${ARTIFACT_PATH}"

# Prepare the services configuration file
SERVICES_CONFIG_FILE="services.toml"
SERVICE_NAME="cryptocurrency-demo"
echo "[user_services]" > ${SERVICES_CONFIG_FILE}
echo "${SERVICE_NAME} = '${ARTIFACT_PATH}'" >> ${SERVICES_CONFIG_FILE}

header "PREPARE PATHS"

EJB_LOG_CONFIG_PATH="${EJB_APP_DIR}/log4j-fallback.xml"
JAVA_LIBRARY_PATH="${EJB_ROOT}/core/rust/target/debug"

# Clear test dir
rm -rf testnet
mkdir testnet

# Enable predefined native logging configuration,
# unless it is already set to any value (incl. null)
# Use jni=error as we currently have some warnings (see ECR-2482).
export RUST_LOG="${RUST_LOG-warn,exonum=info,exonum-java=info,java_bindings=info,jni=error}"

header "GENERATE COMMON CONFIG"
cargo +$RUST_COMPILER_VERSION run -- generate-template --validators-count=1 testnet/common.toml

header "GENERATE CONFIG"
cargo +$RUST_COMPILER_VERSION run -- generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
 --no-password \
 --consensus-path testnet/consensus.toml \
 --service-path testnet/service.toml \
 --peer-address 127.0.0.1:5400

header "FINALIZE"
cargo +$RUST_COMPILER_VERSION run -- finalize testnet/sec.toml testnet/node.toml \
 --public-configs testnet/pub.toml

header "START TESTNET"
cargo +$RUST_COMPILER_VERSION run -- run -d testnet/db -c testnet/node.toml \
 --consensus-key-pass pass \
 --service-key-pass pass \
 --public-api-address 127.0.0.1:3000 \
 --ejb-log-config-path $EJB_LOG_CONFIG_PATH \
 --ejb-port 7000 \
 --ejb-java-library-path $JAVA_LIBRARY_PATH
