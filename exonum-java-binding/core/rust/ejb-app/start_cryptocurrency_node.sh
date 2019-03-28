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

# Use an already set JAVA_HOME, or infer it from java.home system property.
#
# Unfortunately, a simple `which java` will not work for some users (e.g., jenv),
# hence this a bit complex thing.
JAVA_HOME="${JAVA_HOME:-$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')}/"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9)
JVM_LIB_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
echo "JVM_LIB_PATH=${JVM_LIB_PATH}"

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

export LD_LIBRARY_PATH="$JVM_LIB_PATH"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

# Clear test dir
rm -rf testnet
mkdir testnet

header "GENERATE COMMON CONFIG"
cargo run -- generate-template --validators-count=1 testnet/common.toml \
 --ejb-module-name 'com.exonum.binding.cryptocurrency.ServiceModule'

header "GENERATE CONFIG"
cargo run -- generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
 --no-password \
 --consensus-path testnet/consensus.toml \
 --service-path testnet/service.toml \
 --peer-address 127.0.0.1:5400

header "FINALIZE"
cargo run -- finalize testnet/sec.toml testnet/node.toml \
 --ejb-service-classpath $EJB_CLASSPATH \
 --public-configs testnet/pub.toml

header "START TESTNET"
cargo run -- run -d testnet/db -c testnet/node.toml \
 --consensus-key-pass pass \
 --service-key-pass pass \
 --public-api-address 127.0.0.1:3000 \
 --ejb-log-config-path $EJB_LOG_CONFIG_PATH \
 --ejb-port 6000
