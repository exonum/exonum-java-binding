#!/usr/bin/env bash
# Run all tests, generate code coverage report and post it on coveralls.

# Run all java tests, native unit tests and native integration tests.
./run_all_tests.sh

# Generate a coverage report.
mvn jacoco:report coveralls:report
