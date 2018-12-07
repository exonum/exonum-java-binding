#!/usr/bin/env bash
# Runs EJB App tests (ejb-core/rust/ejb-app).
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
#set -eu -o pipefail

# Import necessary environment variables (see the tests_profile header comment for details).
source tests_profile

cd exonum-java-binding-core/rust

cargo "+${RUST_COMPILER_VERSION}" test \
  --manifest-path ejb-app/Cargo.toml
