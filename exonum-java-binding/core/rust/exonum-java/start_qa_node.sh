#!/usr/bin/env bash
# Runs exonum-java executable from target directory and starts N Exonum nodes with
# QA service, where N corresponds to the first argument of the script.
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

java_library_path="${EJB_ROOT}/core/rust/target/debug"

# Find the artifact
ARTIFACT_PATH="$(find ${EJB_ROOT} -type f -name exonum-java-binding-qa-service-*-artifact.jar)"
echo "ARTIFACT_PATH=${ARTIFACT_PATH}"

# Prepare the services configuration file
SERVICES_CONFIG_FILE="services.toml"
SERVICE_NAME="qa"
echo "[user_services]" > ${SERVICES_CONFIG_FILE}
echo "${SERVICE_NAME} = '${ARTIFACT_PATH}'" >> ${SERVICES_CONFIG_FILE}

header "PREPARE PATHS"


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
cargo +$RUST_COMPILER_VERSION run -- generate-template --validators-count $node_count testnet/common.toml

header "GENERATE CONFIG"
for i in $(seq 0 $((node_count - 1)))
do
    peer_port=$((5400 + i))
    log_config_path="$EJB_APP_DIR/testnet/log4j_$i.xml"
    cargo +$RUST_COMPILER_VERSION run -- generate-config testnet/common.toml testnet/pub_$i.toml testnet/sec_$i.toml \
     --no-password \
     --consensus-path testnet/consensus${i}.toml \
     --service-path testnet/service${i}.toml \
     --peer-address 127.0.0.1:$peer_port
done

header "FINALIZE"
for i in $(seq 0 $((node_count - 1)))
do
    cargo +$RUST_COMPILER_VERSION run -- finalize testnet/sec_$i.toml testnet/node_$i.toml \
     --public-configs testnet/pub_*.toml
done

header "START TESTNET"

for i in $(seq 0 $((node_count - 1)))
do
    port=$((3000 + i))
    private_port=$((port + 100))
    ejb_port=$((7000 + i))
    cargo +$RUST_COMPILER_VERSION run -- run \
     -c testnet/node_$i.toml \
     -d testnet/db/$i \
     --ejb-port ${ejb_port} \
     --ejb-log-config-path $log_config_path \
     --consensus-key-pass pass \
     --service-key-pass pass \
     --public-api-address 0.0.0.0:${port} \
     --private-api-address 0.0.0.0:${private_port} \
     --ejb-java-library-path ${java_library_path} &

    echo "new node with ports: $port (public) and $private_port (private)"
done

echo "$node_count nodes configured and launched"

while true; do
    sleep 300
done
