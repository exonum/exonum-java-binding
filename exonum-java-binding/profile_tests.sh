#!/usr/bin/env bash

## No parallel — our baseline
mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -Dprofile="No opts/SeqBuild/SeqTests" -DrunJavaTestsInParallel=false

## Parallel
### No extra options
#### Seq build
mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -Dprofile="No opts/SeqBuild/ParTests" -DrunJavaTestsInParallel=true

#### Parallel build
mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -Dprofile="No opts/ParBuild/ParTests" -DrunJavaTestsInParallel=true -T 1C

### Extra options
export MAVEN_OPTS='-XX:+UseParallelGC'
mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -Dprofile="Options=${MAVEN_OPTS}/ParBuild/ParTests" -DrunJavaTestsInParallel=true -T 1C

export MAVEN_OPTS='-XX:TieredStopAtLevel=1'
mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -Dprofile="Options=${MAVEN_OPTS}/ParBuild/ParTests" -DrunJavaTestsInParallel=true -T 1C

export MAVEN_OPTS='-XX:TieredStopAtLevel=1 -XX:+UseParallelGC'
mvn clean test-compile -q -P ci-build
mvn install -o -P ci-build -Dprofile="Options=${MAVEN_OPTS}/ParBuild/ParTests" -DrunJavaTestsInParallel=true -T 1C