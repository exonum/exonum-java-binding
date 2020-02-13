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

    # Set CI Maven arguments. They set batch mode (less verbose Maven output);
    # and enable parallel builds, and parallel tests (Linux only).
    # TODO: Enable on macos when it uses newer JDK that does not hang up when
    #   parallel tests are enabled.
    MAVEN_CONFIG="--batch-mode"
    if [[ ${TRAVIS_OS_NAME} == "linux" ]]; then
      MAVEN_CONFIG="${MAVEN_CONFIG} \
--threads 1C \
-Djunit.jupiter.execution.parallel.enabled=true \
-Djunit.jupiter.execution.parallel.mode.default=concurrent"
    fi
    echo "${MAVEN_CONFIG}" > .mvn/maven.config

    # Run all tests
    ./run_all_tests.sh

    # Upload the coverage report to Coveralls from a single job only
    if [[ "${TRAVIS_JOB_NAME}" == "Linux JDK 8 CHECK_RUST=false" ]]; then
      echo "PASS"
      # mvn org.eluder.coveralls:coveralls-maven-plugin:report
    fi

    if [[ "${TRAVIS_JOB_NAME}" == "Linux JDK 8 CHECK_RUST=false" ]]; then
      cd "${TRAVIS_BUILD_DIR}/exonum-java-binding/exonum_launcher_java_plugins"
      curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
      python3.7 get-pip.py --user
      git clone https://github.com/exonum/exonum-launcher.git
      pip3 install --user -r exonum-launcher/requirements.txt
      pip3 install --user -e exonum-launcher --no-binary=protobuf protobuf
      pip3 install --user -e .
      wget https://github.com/protocolbuffers/protobuf/releases/download/v3.11.3/protoc-3.11.3-linux-x86_64.zip
      unzip protoc-3.11.3-linux-x86_64.zip
      export PROTOC="$(pwd)/bin/protoc"
      cd tests
      python3.7 -m unittest -v
    fi
fi
