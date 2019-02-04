#!/usr/bin/env bash
# Runs all tests.
#
# A JVM will be selected by JAVA_HOME environment variable, or, if it is not set,
# inferred from the java executable available on the path.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Run all java tests and native unit tests.
./run_maven_tests.sh

# Run native integration tests that require prepared classpaths for fake classes.
./run_native_integration_tests.sh --skip-compile
./run_ejb_app_tests.sh
