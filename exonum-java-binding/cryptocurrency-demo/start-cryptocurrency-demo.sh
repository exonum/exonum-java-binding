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

# EXONUM_HOME shoudl be set to the root directory of Exonum Java App installed
if [ -z "${EXONUM_HOME+x}" ];
then
    echo "ERROR: EXONUM_HOME variable is empty. Please point the EXONUM_HOME to the root of installed Exonum Java App."
    exit 1
fi
echo "EXONUM_HOME=${EXONUM_HOME}"

EXONUM_JAVA_APP="${EXONUM_HOME}/exonum-java"

# Find the latest version of artifact and build if not found.
ARTIFACT_PATH="$(find ./target -type f -name exonum-java-binding-cryptocurrency-demo-*-artifact.jar)"

if [ -z "${ARTIFACT_PATH}+x" ];
then
    mvn install
    ARTIFACT_PATH="$(find ./target -type f -name exonum-java-binding-cryptocurrency-demo-*-artifact.jar)"
fi
echo "ARTIFACT_PATH=${ARTIFACT_PATH}"

# Prepare the services configuration file
SERVICES_CONFIG_FILE="services.toml"
SERVICE_NAME="cryptocurrency-demo"
echo "[user_services]" > ${SERVICES_CONFIG_FILE}
echo "${SERVICE_NAME} = '${ARTIFACT_PATH}'" >> ${SERVICES_CONFIG_FILE}

#TODO: Can be removed after ECR-3030
EJB_LOG_CONFIG_PATH="${EXONUM_HOME}/log4j-fallback.xml"
echo EJB_LOG_CONFIG_PATH=${EJB_LOG_CONFIG_PATH}

# Find the directory containing libjvm.
JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')"
JAVA_LIB_DIR="$(find ${JAVA_HOME} -type f -name libjvm.\* | xargs -n1 dirname)"
export LD_LIBRARY_PATH="$JAVA_LIB_DIR"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

# Clear test dir
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
 --ejb-log-config-path $EJB_LOG_CONFIG_PATH \
 --ejb-port 6000
