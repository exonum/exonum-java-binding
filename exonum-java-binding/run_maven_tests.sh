#!/usr/bin/env bash
# Run all java tests and native unit tests.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Import necessary environment variables (see the tests_profile header comment for details).
source tests_profile

# Run unit and integration tests in ci-build profile. This profile includes:
#  - Java unit & integration tests, including ci-only & slow non-critical tests,
#    which are excluded in the default profile.
#  - Checkstyle checks as errors.
#  - Native unit & integration tests that do not require a JVM.
#  - Test coverage information collection.
# See build definitions of the modules for more.
mvn install \
  --activate-profiles ci-build \
  -Drust.compiler.version="${RUST_COMPILER_VERSION}"
