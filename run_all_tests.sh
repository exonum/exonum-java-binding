#!/usr/bin/env bash
# Builds the project and runs all tests in all sub-projects.
# This file should be extended in case new custom (not maven) tests appear.
# This file should be always synchronized with exonum-java-binding/run_all_tests.sh for
# correct work.
#
# A JVM will be selected by JAVA_HOME environment variable, or, if it is not set,
# inferred from the java executable available on the path.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Import necessary environment variables (see the tests_profile header comment for details).
source exonum-java-binding/tests_profile

echo "Start building the project with running all Java test"
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

echo "Start running EJB native tests"
cd exonum-java-binding
# Run native integration tests that require prepared classpaths for fake classes.
./run_native_integration_tests.sh --skip-compile
./run_app_tests.sh
