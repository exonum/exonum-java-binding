#!/usr/bin/env bash
# Runs exonum-java executable from target directory and starts a single node, using
# either qa-service/target or cryptocurrency-demo/target as artifacts directory.
#
# Usage: start_node.sh [qa|cryptocurrency]
#
# This script is intended to be used by Exonum developers to speed up workflow.
# Running the executable from target doesn't need packaging and therefore can be performed
# immediately after running tests, without additional recompilation.
#
# To start cryptocurrency service, use the following command:
#   python3 -m exonum_launcher -i ../../../cryptocurrency-demo/cryptocurrency-demo.yml
#
# To start QA service, use the following command:
#   python3 -m exonum_launcher -i qa-service.yml

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

ARTIFACTS_PATH=""

if [[ $# -eq 0 ]] ; then
    echo "Usage: start_node.sh [qa|cryptocurrency]" >&2
    exit 1
fi

while (( "$#" )); do
  case "$1" in
    qa)
      ARTIFACTS_PATH="${EJB_ROOT}/qa-service/target"
      shift 1
      ;;
    cryptocurrency)
      ARTIFACTS_PATH="${EJB_ROOT}/cryptocurrency-demo/target"
      shift 1
      ;;
    *) # anything else
      echo "Usage: start_node.sh [qa|cryptocurrency]" >&2
      exit 1
      ;;
  esac
done

header "PREPARE PATHS"

TESTNET_DIRECTORY="testnet"
COMMON_CONFIG_PATH="${TESTNET_DIRECTORY}/common.toml"
SEC_CONFIG_PATH="${TESTNET_DIRECTORY}/sec.toml"
PUB_CONFIG_PATH="${TESTNET_DIRECTORY}/pub.toml"
NODE_CONFIG_PATH="${TESTNET_DIRECTORY}/node.toml"
DATABASE_PATH="${TESTNET_DIRECTORY}/db"

EJB_LOG_CONFIG_PATH="${EJB_APP_DIR}/log4j-fallback.xml"
JAVA_LIBRARY_PATH="${EJB_ROOT}/core/rust/target/debug/deps"

# Clear test dir
rm -rf ${TESTNET_DIRECTORY}
mkdir -p ${TESTNET_DIRECTORY}

# Delete the java_bindings library from the target/debug if any to prevent ambiguity in dynamic linking (ECR-3468)
rm -f ${EJB_ROOT}/core/rust/target/debug/libjava_bindings.*

header "COMPILE EXONUM-JAVA"
cargo +${RUST_COMPILER_VERSION} build

# Enable predefined native logging configuration,
# unless it is already set to any value (incl. null)
export RUST_LOG="${RUST_LOG-warn,exonum=info,exonum-java=info,java_bindings=info}"

header "GENERATE COMMON CONFIG"
cargo +${RUST_COMPILER_VERSION} run -- generate-template \
    --validators-count=1 \
    ${COMMON_CONFIG_PATH}

header "GENERATE CONFIG"
cargo +${RUST_COMPILER_VERSION} run -- generate-config \
    ${COMMON_CONFIG_PATH} \
    ${TESTNET_DIRECTORY} \
    --no-password \
    --peer-address 127.0.0.1:5400

header "FINALIZE"
cargo +${RUST_COMPILER_VERSION} run -- finalize \
    ${SEC_CONFIG_PATH} \
    ${NODE_CONFIG_PATH} \
    --public-configs ${PUB_CONFIG_PATH}

header "START TESTNET"
cargo +${RUST_COMPILER_VERSION} run -- run \
    --artifacts-path ${ARTIFACTS_PATH} \
    --node-config ${NODE_CONFIG_PATH} \
    --db-path ${DATABASE_PATH} \
    --master-key-pass pass \
    --public-api-address 127.0.0.1:3000 \
    --private-api-address 127.0.0.1:3010 \
    --ejb-log-config-path ${EJB_LOG_CONFIG_PATH} \
    --ejb-port 7000 \
    --ejb-override-java-library-path ${JAVA_LIBRARY_PATH}
