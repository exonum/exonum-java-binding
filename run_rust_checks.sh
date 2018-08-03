#!/usr/bin/env bash
#
# If CHECK_RUST is true, performs rust code checks,
# otherwise skips checking rust code

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
fi
