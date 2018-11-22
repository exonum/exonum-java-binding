#!/usr/bin/env bash
set -eu -o pipefail
source init_env.sh

node_id=$1

header "RUN NODE #${node_id}"

# A quick check that we don't use wrong ids
if [[ $node_id = "0" ]]; then
echo "Node identifiers must start with 1"
exit 1
fi

public_port=$((3000 + node_id))
private_port=$((3100 + node_id))

# Start node with the given id
cargo run -- run \
    -c ../testnet/node_$node_id.toml \
    -d ../testnet/db/$node_id \
    --public-api-address 0.0.0.O:${public_port}
