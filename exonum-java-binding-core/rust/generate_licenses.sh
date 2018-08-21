#!/usr/bin/env bash
# Gather licenses for every Rust dependency and print it in the file in a readable form.

cargo license -h &> /dev/null || cargo install cargo-license

# Going to exonum-java-binding-core/rust directory.
backup_dir=${PWD}
script_dir=$(dirname $(readlink -f "$0"))
cd $(dirname $0)

cargo license -da \
| sed 's@registry+https://github.com/rust-lang/crates.io-index, @@g' \
| sed 's/\x1b\[[0-9;]*m//g' \
> "${script_dir}/target/LICENSES-THIRD-PARTY-NATIVE"

# Returning to root EJB directory.
cd ${backup_dir}