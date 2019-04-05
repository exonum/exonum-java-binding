#!/usr/bin/env bash
# Runs Exonum Java app tests (ejb-core/rust/exonum-java).
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Import necessary environment variables (see the tests_profile header comment for details).
source tests_profile

cd core/rust

cargo "+${RUST_COMPILER_VERSION}" test \
  --manifest-path exonum-java/Cargo.toml
