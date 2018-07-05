#!/usr/bin/env bash
# Runs native integration tests (those in ejb-core/rust/integration_tests).
# If --skip-compile is passed, does not recompile all Java sources.
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables.
set -eu -o pipefail

# Import necessary environment variables (see the tests_profile header comment for details).
source tests_profile

# Compile all Java modules by default to ensure that ejb-fakes module, which is required
# by native ITs, is up-to-date. This safety net takes about a dozen seconds,
# so if the Java artefacts are definitely up-to-date, it may be skipped.
if [ "$#" -eq 0 ]; then
  # Compile Java artefacts.
  echo "Compiling the Java artefacts…"
  mvn -DskipTests compile --quiet
else
  if [ "$1" != "--skip-compile" ]; then
    echo "Unknown option: $1"
    exit 1
  fi
fi

cd exonum-java-binding-core/rust

# Stable works well unless you want benchmarks.
RUST_COMPILER_VERSION="stable"

cargo "+${RUST_COMPILER_VERSION}" test \
  --manifest-path integration_tests/Cargo.toml
