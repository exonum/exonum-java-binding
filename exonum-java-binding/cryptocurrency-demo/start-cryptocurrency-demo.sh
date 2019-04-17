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

# Check that path to the JVM library is set.
if [ -z "${JAVA_LIB_DIR+x}" ];
then
    echo "ERROR: JAVA_LIB_DIR variable is not set. Please make sure you executed 'source ../tests_profile'"
    exit 1
fi

export LD_LIBRARY_PATH="${JAVA_LIB_DIR}"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

# Find the latest version of artifact and build if not found.
ARTIFACT_PATH="$(find ./target -type f -name exonum-java-binding-cryptocurrency-demo-*-artifact.jar)"

if [ -z "${ARTIFACT_PATH}" ];
then
    mvn package
    ARTIFACT_PATH="$(find ./target -type f -name exonum-java-binding-cryptocurrency-demo-*-artifact.jar)"
fi
echo "ARTIFACT_PATH=${ARTIFACT_PATH}"

# Prepare the services configuration file.
SERVICES_CONFIG_FILE="services.toml"
SERVICE_NAME="cryptocurrency-demo"
echo "[user_services]" > ${SERVICES_CONFIG_FILE}
echo "${SERVICE_NAME} = '${ARTIFACT_PATH}'" >> ${SERVICES_CONFIG_FILE}

# Clear test dir.
rm -rf testnet
mkdir testnet

header "GENERATE COMMON CONFIG"
${EXONUM_JAVA_APP} generate-template --validators-count=1 testnet/common.toml

header "GENERATE CONFIG"
${EXONUM_JAVA_APP} generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
 --no-password \
 --consensus-path testnet/consensus.toml \
 --service-path testnet/service.toml \
 --peer-address 127.0.0.1:5400

header "FINALIZE"
${EXONUM_JAVA_APP} finalize testnet/sec.toml testnet/node.toml \
 --public-configs testnet/pub.toml

header "START TESTNET"
${EXONUM_JAVA_APP} run -d testnet/db -c testnet/node.toml \
 --consensus-key-pass pass \
 --service-key-pass pass \
 --public-api-address 127.0.0.1:3000 \
 --ejb-port 6000
