#!/usr/bin/env bash
# Runs all tests.
#
# A JVM will be selected by JAVA_HOME environment variable, or, if it is not set,
# inferred from the java executable available on the path.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Run rust code checks if CHECK_RUST is true, or java tests if it's not
if [ "$CHECK_RUST" = true ] 
then
    # Install cargo-audit if it's not already.
    cargo-audit -V || cargo install cargo-audit --force
    # Install nightly rust version and clippy.
    rustup toolchain install $RUST_NIGHTLY_VERSION
    cargo +$RUST_NIGHTLY_VERSION clippy -V | grep $RUST_CLIPPY_VERSION || cargo +$RUST_NIGHTLY_VERSION install clippy --vers $RUST_CLIPPY_VERSION --force
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
    # Check silently for updates of dependencies
    mvn versions:display-property-updates versions:display-dependency-updates | grep '\->' --context=3 || true
    echo 'Rust checks are completed.'
else
    cd "${TRAVIS_BUILD_DIR}"
    ./run_all_tests.sh
fi
