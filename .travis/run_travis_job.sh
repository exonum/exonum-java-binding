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
    # Install cargo-audit if it's not already.
    cargo audit --version || cargo install cargo-audit --force
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

    # Run audit of vulnerable dependencies.
    cargo audit | true

    # Check silently for updates of Maven dependencies.
    # TODO Disabled until ECR-2252 is fixed.
    #cd "${TRAVIS_BUILD_DIR}"
    #mvn versions:display-property-updates versions:display-dependency-updates | grep '\->' --context=3 || true

    echo 'Rust checks are completed.'
else
    cd "${TRAVIS_BUILD_DIR}"

    ./run_all_tests.sh;
    # Linux builds currently skip some tests, so only OSX builds should update code coverage report.
    if [[ "$TRAVIS_OS_NAME" == "osx" ]]; then
      # Upload the coverage report to coveralls
      mvn org.eluder.coveralls:coveralls-maven-plugin:report
    fi
fi
