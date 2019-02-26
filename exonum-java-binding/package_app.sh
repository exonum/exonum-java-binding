#!/usr/bin/env bash
# Package EJB App after preparing necessary environment variables and file structure.
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

function build-ejb-app-macos() {
    # Set RUSTFLAGS to adjust RUNPATH of the binary.
    export RUSTFLAGS="-C link-arg=-Wl,-rpath,@executable_path/lib/native"
    echo "RUSTFLAGS=${RUSTFLAGS}"
    mvn package --activate-profiles app-packaging -pl :exonum-java-binding-core -am -DskipTests \
      -Drust.libraryPath="$(pwd)/core/rust/target/debug/libjava_bindings.dylib"
}

function build-ejb-app-linux() {
    # Set RUSTFLAGS to adjust RUNPATH of the binary.
    export RUSTFLAGS="-C link-arg=-Wl,-rpath,\$ORIGIN/lib/native/"
    echo "RUSTFLAGS=${RUSTFLAGS}"
    mvn package --activate-profiles app-packaging -pl :exonum-java-binding-core -am -DskipTests \
      -Drust.libraryPath="$(pwd)/core/rust/target/debug/libjava_bindings.so"
}

EJB_RUST_DIR="${PWD}/core/rust"

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
export RUST_COMPILER_VERSION="${RUST_COMPILER_VERSION:-stable}"
echo "RUST_COMPILER_VERSION: ${RUST_COMPILER_VERSION}"

# Find the directory containing Rust libstd.
export RUST_LIB_DIR=$(rustup run ${RUST_COMPILER_VERSION} rustc --print sysroot)/lib
echo "RUST_LIB_DIR: ${RUST_LIB_DIR}"

# Checking if RUSTFLAGS has already been set.
if [[ "${RUSTFLAGS:-}" != "" ]]; then
    echo "Warning: the RUSTFLAGS variable will be overriden. Merge is not yet supported."
fi

# Set LD_LIBRARY_PATH as needed for building and testing.
export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:-}:${RUST_LIB_DIR}:${JAVA_LIB_DIR}"

# Copy libstd to some known place.
PREPACKAGE_DIR="${EJB_RUST_DIR}/target/prepackage"
mkdir -p "${PREPACKAGE_DIR}"
cp ${RUST_LIB_DIR}/libstd* "${PREPACKAGE_DIR}"

# Copy licenses so that the package tool can pick them up.
cp ../LICENSE "${PREPACKAGE_DIR}"
cp LICENSES-THIRD-PARTY.TXT "${PREPACKAGE_DIR}"

# Generate licenses for native dependencies.
./core/rust/generate_licenses.sh

if [[ "$(uname)" == "Darwin" ]]; then
    build-ejb-app-macos
elif [[ "$(uname -s)" == Linux* ]]; then
    build-ejb-app-linux
fi
