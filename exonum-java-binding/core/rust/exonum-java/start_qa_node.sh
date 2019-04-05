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
JAVA_HOME="${JAVA_HOME:-$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')}"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9)
export LD_LIBRARY_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"

EJB_APP_DIR=$(pwd)
echo "CURRENT_DIR=${EJB_APP_DIR}"

EJB_ROOT=$(realpath "../../..")
echo "PROJ_ROOT=${EJB_ROOT}"

# Find the artifact
ARTIFACT_PATH="$(find ${EJB_ROOT} -type f -name exonum-java-binding-qa-service-*-artifact.jar)"
echo "ARTIFACT_PATH=${ARTIFACT_PATH}"

# Prepare the services configuration file
SERVICES_CONFIG_FILE="services.toml"
SERVICE_NAME="ejb-qa-service"
echo "[user_services]" > ${SERVICES_CONFIG_FILE}
echo "${SERVICE_NAME} = '${ARTIFACT_PATH}'" >> ${SERVICES_CONFIG_FILE}

header "PREPARE PATHS"

echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

# Clear test dir
rm -rf testnet
mkdir testnet

trap "killall exonum-java" SIGINT SIGTERM EXIT

# Configure and run nodes
node_count=$1

header "PREPARE LOG CONFIGS"
rm -rf logs
mkdir logs
for i in $(seq 0 $((node_count -1)))
do
    log_config_path="$EJB_APP_DIR/testnet/log4j_$i.xml"
    log_file_path="$EJB_APP_DIR/logs/log_$i.txt"
    sed "s@FILENAME@$log_file_path@g" "$EJB_APP_DIR/log4j_template.xml" > "$log_config_path"
done

header "GENERATE COMMON CONFIG"
cargo run -- generate-template --validators-count $node_count testnet/common.toml

header "GENERATE CONFIG"
for i in $(seq 0 $((node_count - 1)))
do
    peer_port=$((5400 + i))
    log_config_path="$EJB_APP_DIR/testnet/log4j_$i.xml"
    cargo run -- generate-config testnet/common.toml testnet/pub_$i.toml testnet/sec_$i.toml \
     --no-password \
     --consensus-path testnet/consensus${i}.toml \
     --service-path testnet/service${i}.toml \
     --peer-address 127.0.0.1:$peer_port
done

header "FINALIZE"
for i in $(seq 0 $((node_count - 1)))
do
    cargo run -- finalize testnet/sec_$i.toml testnet/node_$i.toml \
     --public-configs testnet/pub_*.toml
done

header "START TESTNET"

for i in $(seq 0 $((node_count - 1)))
do
    port=$((3000 + i))
    private_port=$((port + 100))
    ejb_port=$((6000 + i))
    cargo run -- run \
     -c testnet/node_$i.toml \
     -d testnet/db/$i \
     --ejb-port ${ejb_port} \
     --ejb-log-config-path $log_config_path \
     --consensus-key-pass pass \
     --service-key-pass pass \
     --public-api-address 0.0.0.0:${port} \
     --private-api-address 0.0.0.0:${private_port} &

    echo "new node with ports: $port (public) and $private_port (private)"
done

echo "$node_count nodes configured and launched"

while true; do
    sleep 300
done
