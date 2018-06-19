#!/usr/bin/env bash
# Gather licenses for every Rust dependency and print it in the file in a readable form.

cargo license -h || cargo install cargo-license

cargo license -da | sed 's@registry+https://github.com/rust-lang/crates.io-index, @@g' | sed 's/\x1b\[[0-9;]*m//g' > target/LICENSES-THIRD-PARTY-NATIVE

