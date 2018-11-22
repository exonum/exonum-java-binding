#!/usr/bin/env bash

set -eu -o pipefail
source ./init_env.sh

header "NODES CONFIGURATION"

num_nodes=$1
# Clear the test dir

header "Re-create testnet folder"
rm -rf ../testnet
mkdir ../testnet


header "Step 1: Generate a template for a network of num nodes"
cargo run -- generate-template \
  --validators-count="${num_nodes}" \
  ../testnet/common.toml

header "Step 2: Generate a node public and private configuration"

CORE_TXT="exonum-java-binding-core/target/ejb-core-classpath.txt"
CRYPTOCURRENCY_TXT="exonum-java-binding-cryptocurrency-demo/target/cryptocurrency-classpath.txt"
EJB_CLASSPATH="$(cat ${EJB_ROOT}/${CORE_TXT}):$(cat ${EJB_ROOT}/${CRYPTOCURRENCY_TXT})"
EJB_CLASSPATH="${EJB_CLASSPATH}:${EJB_ROOT}/exonum-java-binding-core/target/classes"
EJB_CLASSPATH="${EJB_CLASSPATH}:${EJB_ROOT}/exonum-java-binding-cryptocurrency-demo/target/classes"
EJB_LIBPATH="${EJB_ROOT}/exonum-java-binding-core/rust/target/debug/"
EJB_LOG_CONFIG_PATH="${EJB_ROOT}/exonum-java-binding-core/rust/ejb-app/log4j2.xml"


for node_id in $(seq 1 $((num_nodes)))
do
    peer_port=$((5400 + node_id))
    cargo run -- generate-config \
        ../testnet/common.toml \
        ../testnet/pub_${node_id}.toml \
        ../testnet/sec_${node_id}.toml \
        --ejb-classpath $EJB_CLASSPATH	\
        --ejb-libpath $EJB_LIBPATH \
        --ejb-log-config-path $EJB_LOG_CONFIG_PATH \
        --peer-address "127.0.0.1:${peer_port}"
done

header "Step 3: Finalize the configuration"
for node_id in $(seq 1 $((num_nodes)))
do
    crypto_service_port=$((6000	+ node_id))
    cargo run -- finalize \
        ../testnet/sec_${node_id}.toml \
        ../testnet/node_${node_id}.toml \
        --ejb-module-name 'com.exonum.binding.cryptocurrency.ServiceModule' \
        --ejb-port $crypto_service_port \
        --public-configs ../testnet/pub_*.toml
done