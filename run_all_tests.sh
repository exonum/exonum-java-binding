#!/usr/bin/env bash
# Runs all tests.
#

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

echo "Start running all EJB tests"
cd exonum-java-binding-parent
./run_all_tests.sh
