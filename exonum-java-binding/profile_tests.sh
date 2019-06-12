#!/usr/bin/env bash

## Parallel

mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -DrunJavaTestsInParallel=true -Dprofile

## No parallel
mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -DrunJavaTestsInParallel=false -Dprofile