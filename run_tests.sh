#!/usr/bin/env bash
# Runs all tests.
#
# A JVM will be selected by JAVA_HOME environment variable, or, if it is not set,
# inferred from the java executable available on the path.
# 
# If CHECK_RUST is true, performs rust code checks only,
# skipping java tests. Runs java tests skipping checking rust code
# otherwise.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

source tests_profile # this used by run_maven_tests and native_integration_tests

# ex- ./run_maven_tests.sh
# Run all java tests and native unit tests.
run_maven_tests() {
    # Import necessary environment variables (see the tests_profile header comment for details).
    # Run unit and integration tests in ci-build profile. This profile includes:
    #  - Java unit & integration tests, including ci-only & slow non-critical tests,
    #    which are excluded in the default profile.
    #  - Checkstyle checks as errors.
    #  - Native unit & integration tests that do not require a JVM.
    # See build definitions of the modules for more.
    mvn install \
      --activate-profiles ci-build \
      -Drust.compiler.version="${RUST_COMPILER_VERSION}"
}

# ex- ./run_native_integration_tests.sh --skip-compile
# Run native integration tests that require prepared classpaths for fake classes.
run_native_integration_tests() {
    if [ "$SKIP_COMPILE" != true ]; then
        # Compile Java artefacts.
        echo "Compiling the Java artefacts…"
        mvn compile --quiet
    fi

    cd "${EJB_RUST_BUILD_DIR}"
    cargo "+${RUST_COMPILER_VERSION}" test \
      --manifest-path integration_tests/Cargo.toml

}

# ex- ./run_ejb_app_tests.sh
run_ejb_app_tests() {
    cd "${EJB_RUST_BUILD_DIR}"
    cargo "+${RUST_COMPILER_VERSION}" test --manifest-path ejb-app/Cargo.toml
}

check_for_deps_updates() {
    # Check silently for updates of dependencies
    mvn versions:display-property-updates versions:display-dependency-updates | grep '\->' --context=3 || true

}

install_rust_nightly() {
    # Install cargo-audit if it's not already.
    cargo-audit -V || cargo install cargo-audit --force
    # Install nightly rust version and clippy.
    rustup toolchain install $RUST_NIGHTLY_VERSION
    cargo +$RUST_NIGHTLY_VERSION clippy -V | grep $RUST_CLIPPY_VERSION || cargo +$RUST_NIGHTLY_VERSION install clippy --vers $RUST_CLIPPY_VERSION --force
}

perform_rust_checks() {
    echo 'Performing checks over the rust code'
    cd "${EJB_RUST_BUILD_DIR}"
    # Install rustfmt.
    # Use stable Rust for rustfmt.
    # TODO: use stable rust everywhere when ECR-1839 fixed.
    rustup toolchain install stable
    rustup component add rustfmt-preview --toolchain stable
    rustup run stable rustfmt -V
    cargo +stable fmt --all -- --check
    # TODO Remove when clippy is fixed https://github.com/rust-lang-nursery/rust-clippy/issues/2831
    # Next 2 lines are a workaround to prevent clippy checking dependencies.
    cargo +${RUST_NIGHTLY_VERSION} check
    cargo +${RUST_NIGHTLY_VERSION} clean -p java_bindings
    cargo +${RUST_NIGHTLY_VERSION} clippy --all --tests --all-features -- -D warnings
    # TODO ignoring cargo audit until ECR-1902 is fixed
    cargo audit || true
    echo 'done.'
}

skip_rust_checks() {
    echo 'Skipping rust code checks'
}

if [ "$CHECK_RUST" = true ] 
then
    install_rust_nightly
    perform_rust_checks
else
    skip_rust_checks
    run_maven_tests
    run_native_integration_tests
    run_ejb_app_tests
    check_for_deps_updates
fi
