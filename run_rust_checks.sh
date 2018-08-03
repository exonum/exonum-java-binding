#!/usr/bin/env bash
#
# If CHECK_RUST is true, performs rust code checks,
# otherwise skips checking rust code

perform_rust_checks() {
    echo 'Performing checks over the rust code'
    # echo $CHECK_RUST
    export PATH="$PATH":"$HOME/.cargo/bin"
    # Install rustup if it's not already installed (i.e., not in CI cache).
    which rustup > /dev/null || curl https://sh.rustup.rs -sSf | sh -s -- -y --default-toolchain "$RUST_COMPILER_VERSION"
    # Install nightly rust version and clippy.
    rustup toolchain install $RUST_NIGHTLY_VERSION
    cargo +$RUST_NIGHTLY_VERSION clippy -V | grep $RUST_CLIPPY_VERSION || cargo +$RUST_NIGHTLY_VERSION install clippy --vers $RUST_CLIPPY_VERSION --force
    # Save the path to stdlib of the default toolchain to use it in scripts.
    export RUST_LIB_DIR=$(rustup run "$RUST_VERSION" rustc --print sysroot)/lib
    # Install rustfmt.
    # Use stable Rust for rustfmt.
    # TODO: use stable rust everywhere when ECR-1839 fixed.
    rustup toolchain install stable
    rustup component add rustfmt-preview --toolchain stable
    rustup run stable rustfmt -V
    rustup default "$RUST_COMPILER_VERSION"
    # Install cargo-audit if it's not already.
    cargo-audit -V || cargo install cargo-audit --force
    # List all installed cargo packages.
    cargo install --list
    # libjava_bindings.so and native application both need to load common Rust libs (eg libstd.so).
    export LD_LIBRARY_PATH=$RUST_LIB_DIR:$LD_LIBRARY_PATH
    # force building instead of using from apt.
    export SODIUM_BUILD=1
    export ROCKSDB_BUILD=1
    export SNAPPY_BUILD=1
    cd "${EJB_RUST_BUILD_DIR}"
    cargo +stable fmt --all -- --write-mode=check
    # TODO Remove when clippy is fixed https://github.com/rust-lang-nursery/rust-clippy/issues/2831
    # Next 2 lines are a workaround to prevent clippy checking dependencies.
    cargo +${RUST_NIGHTLY_VERSION} check
    cargo +${RUST_NIGHTLY_VERSION} clean -p java_bindings
    cargo +${RUST_NIGHTLY_VERSION} clippy --all --tests --all-features -- -D warnings
    # TODO ignoring cargo audit until ECR-1902 is fixed
    cargo audit || true
    # cd -
    echo 'done.'
}

skip_rust_checks() {
    echo 'Skipping checks'
}

if [ "$CHECK_RUST" = true ] 
then
    perform_rust_checks
else
    skip_rust_checks
fi
