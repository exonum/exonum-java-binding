#!/usr/bin/env bash
# Package EJB App after preparing necessary environment variables and file structure.
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

EJB_RUST_DIR="${PWD}/exonum-java-binding-core/rust"

# Use active JVM.
#
# Unfortunately, a simple `which java` will not work for some users (e.g., jenv),
# hence this a bit complex thing.
export JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9).
export JAVA_LIB_DIR="$(find ${JAVA_HOME} -type f -name libjvm.\* | xargs -n1 dirname)"
echo "JAVA_LIB_DIR=${JAVA_LIB_DIR}"

# Version of Rust used to locate libstd.
# TODO: stable does not work well until ECR-1839 is resolved
export RUST_COMPILER_VERSION="${RUST_COMPILER_VERSION:-1.26.2}"
echo "RUST_COMPILER_VERSION: ${RUST_COMPILER_VERSION}"

# Find the directory containing Rust libstd.
export RUST_LIB_DIR=$(rustup run ${RUST_COMPILER_VERSION} rustc --print sysroot)/lib
echo "RUST_LIB_DIR: ${RUST_LIB_DIR}"

# Checking if RUSTFLAGS has already been set.
if [[ "${RUSTFLAGS:-}" != "" ]]; then
    echo "Warning: the RUSTFLAGS variable will be overriden. Merge is not yet supported."
fi

# Set RUSTFLAGS to adjust RUNPATH of the binary.
export RUSTFLAGS="-C link-arg=-Wl,-rpath,\$ORIGIN/lib/native"
echo "RUSTFLAGS=${RUSTFLAGS}"

# Set LD_LIBRARY_PATH as needed for building and testing.
export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:-}:${RUST_LIB_DIR}:${JAVA_LIB_DIR}"

# Copy libstd to some known place.
mkdir -p ${EJB_RUST_DIR}/target/prepackage
cp $RUST_LIB_DIR/libstd* ${EJB_RUST_DIR}/target/prepackage

# Recompile native part.
cd ${EJB_RUST_DIR}
cargo build --all

cd ../..

# Copy licenses so that the package tool can pick them up.
cp LICENSE exonum-java-binding-core
cp LICENSES-THIRD-PARTY.TXT exonum-java-binding-core

# Generate licenses for native dependencies.
exonum-java-binding-core/rust/generate_licenses.sh

mvn package --activate-profiles app-packaging -pl exonum-java-binding-core -am
