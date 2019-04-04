#!/usr/bin/env bash
# Runs all tests in the EJB project.
# The main purpose of this file is to run all EJB tests from the EJB parent directory.
# This file should be always synchronized with ../run_all_tests.sh for correct work.
#
# A JVM will be selected by JAVA_HOME environment variable, or, if it is not set,
# inferred from the java executable available on the path.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Run all java tests and native unit tests.
./run_maven_tests.sh

# Run native integration tests that require prepared classpaths for fake classes.
./run_native_integration_tests.sh --skip-compile
run_app_tests.sh
