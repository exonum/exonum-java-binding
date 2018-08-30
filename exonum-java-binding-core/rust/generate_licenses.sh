#!/usr/bin/env bash
# Gather licenses for every Rust dependency and print it in the file in a readable form.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

cargo license -h &> /dev/null || cargo install cargo-license

# Going to exonum-java-binding-core/rust directory.
backup_dir=${PWD}
script_dir=$(dirname $(readlink -f "$0")) # Directory of this script. Must be exonum-java-binding-core/rust
cd ${script_dir}

cargo license -da \
| sed 's@registry+https://github.com/rust-lang/crates.io-index, @@g' `# Remove unnecessary information from the list` \
| sed 's/\x1b\[[0-9;]*m//g' `# Remove color escape sequences from the list` \
> "${script_dir}/target/LICENSES-THIRD-PARTY-NATIVE"

# Returning to the root EJB directory.
cd ${backup_dir}
