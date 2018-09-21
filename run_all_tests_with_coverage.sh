#!/usr/bin/env bash
# Run all tests, generate code coverage report and post it on coveralls.

# Run all java tests, native unit tests and native integration tests.
./run_all_tests.sh

# Generate a coverage report.
mvn org.jacoco:jacoco-maven-plugin:report org.eluder.coveralls:coveralls-maven-plugin:report
