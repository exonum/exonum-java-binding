#!/usr/bin/env bash
# Runs all tests with OpenClover instrumentation:
# http://openclover.org/doc/manual/4.2.0/maven--quick-start-guide.html
#
# This script duplicates some of the operations
# of `run_all_tests.sh` because it needs to compile the instrumented sources
# first, then run native ITs, and only then collect the statistics.

# Fail immediately in case of any errors and/or unset variables
set -eu -o pipefail

# todo: All Java tests (Unit & ITs) are skipped until lib loading is fixed [https://jira.bf.local/browse/ECR-942].
# Run unit and integration tests in ci-build profile. This profile includes:
#  - Java unit & integration tests, including ci-only & slow non-critical tests,
#    which are excluded in the default profile.
#  - Checkstyle checks as errors.
#  - Native unit & integration tests that do not require a JVM.
# See build definitions of the modules for more.
mvn install \
  -DskipTests \
  --activate-profiles ci-build \
  -Drust.compiler.version="stable"

# Run native integration tests that require a JVM.
./run_native_integration_tests.sh --skip-compile

# Generate a coverage report
mvn jacoco:report coveralls:report
