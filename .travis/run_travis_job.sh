#!/usr/bin/env bash
# Runs all tests.
#
# A JVM will be selected by JAVA_HOME environment variable, or, if it is not set,
# inferred from the java executable available on the path.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Echo commands so that the progress can be seen in CI server logs.
set -x

# Run rust code checks if CHECK_RUST is true, or java tests if it's not
if [ "$CHECK_RUST" = true ] 
then
    # Install clippy and rustfmt.
    rustup component add clippy
    rustup component add rustfmt
    rustfmt -V
    cargo clippy -V

    echo 'Performing checks over the rust code'
    EJB_RUST_BUILD_DIR="${TRAVIS_BUILD_DIR}/exonum-java-binding/core/rust/"
    cd "${EJB_RUST_BUILD_DIR}"

    # Check the formatting.
    cargo fmt -- --check

    # Run clippy static analysis.
    cargo clippy --all --tests --all-features -- -D warnings

    echo 'Rust checks are completed.'
else
    cd "${TRAVIS_BUILD_DIR}"

    # Set CI Maven arguments. They enable parallel builds, and parallel tests (Linux only).
    # TODO: Remove this when macos builds use newer JDK that does not hang up when
    #   parallel tests are enabled.
    PARALLEL_TESTS_ENABLED="true"
    if [[ ${TRAVIS_OS_NAME} == "osx" ]]; then
      PARALLEL_TESTS_ENABLED="false"
    fi
    echo "--threads 1C -Djunit.jupiter.execution.parallel.enabled=${PARALLEL_TESTS_ENABLED} \
          -Djunit.jupiter.execution.parallel.mode.default=concurrent" > \
      .mvn/maven.config

    # Run all tests
    ./run_all_tests.sh

    # Upload the coverage report to Coveralls from a single job only
    if [[ "${TRAVIS_JOB_NAME}" == "Linux JDK 8 CHECK_RUST=false" ]]; then
      mvn org.eluder.coveralls:coveralls-maven-plugin:report
    fi
fi
