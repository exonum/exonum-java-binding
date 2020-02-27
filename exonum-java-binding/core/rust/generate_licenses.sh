#!/usr/bin/env bash
# Gather licenses for every Rust dependency and print it in the file in a readable form.
# Resulting file destination is either specified by `PACKAGING_ETC_DIR` variable, or
# is the script directory (exonum-java-binding/core/rust)

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

cargo-license -h &> /dev/null || cargo install cargo-license

# Going to exonum-java-binding/core/rust directory.
backup_dir=${PWD}
script_dir="$(dirname "$(realpath -s "$0")")" # Directory of this script. Must be exonum-java-binding/core/rust
cd "${script_dir}"

cargo-license --json > "${PACKAGING_ETC_DIR:-.}/LICENSES-THIRD-PARTY-NATIVE"

# Restoring initial directory.
cd "${backup_dir}"
