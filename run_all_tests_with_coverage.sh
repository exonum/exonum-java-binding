#!/usr/bin/env bash
# Run all tests, generate code coverage report and post it on coveralls.

# Run all java tests, native unit tests and native integration tests.
./run_all_tests.sh

# Update coveralls coverage report.
mvn org.eluder.coveralls:coveralls-maven-plugin:report
