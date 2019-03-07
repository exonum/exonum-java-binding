#!/usr/bin/env bash

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

header "PREPARE PATHS"

CRYPTOCURRENCY_TXT="cryptocurrency-demo/target/cryptocurrency-classpath.txt"
EJB_CLASSPATH="$(cat ${EJB_ROOT}/${CRYPTOCURRENCY_TXT})"
EJB_CLASSPATH="${EJB_CLASSPATH}:${EJB_ROOT}/cryptocurrency-demo/target/classes"
echo "EJB_CLASSPATH=${EJB_CLASSPATH}"
EJB_LOG_CONFIG_PATH="${EJB_APP_DIR}/log4j-fallback.xml"

# Clear test dir
rm -rf testnet
mkdir testnet

ejb_app="${EJB_ROOT}/core/rust/target/debug/ejb-app"

header "GENERATE COMMON CONFIG"
"${ejb_app}" generate-template --validators-count=1 testnet/common.toml \
 --ejb-module-name 'com.exonum.binding.cryptocurrency.ServiceModule'

header "GENERATE CONFIG"
"${ejb_app}" generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
 --peer-address 127.0.0.1:5400

header "FINALIZE"
"${ejb_app}" finalize testnet/sec.toml testnet/node.toml \
 --ejb-service-classpath $EJB_CLASSPATH \
 --public-configs testnet/pub.toml

header "START TESTNET"
"${ejb_app}" run -d testnet/db -c testnet/node.toml \
 --public-api-address 127.0.0.1:3000 \
 --ejb-log-config-path $EJB_LOG_CONFIG_PATH \
 --ejb-port 6000
