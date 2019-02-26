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
    mvn package --activate-profiles app-packaging -pl :exonum-java-binding-core -am \
      -DskipTests \
      -DdoNotBuildRustLib \
      -Drust.libraryPath="$(pwd)/core/rust/target/debug/libjava_bindings.dylib"
}

function build-ejb-app-linux() {
    # Set RUSTFLAGS to adjust RUNPATH of the binary.
    export RUSTFLAGS="-C link-arg=-Wl,-rpath,\$ORIGIN/lib/native/"
    echo "RUSTFLAGS=${RUSTFLAGS}"
    mvn package --activate-profiles app-packaging -pl :exonum-java-binding-core -am \
      -DskipTests \
      -DdoNotBuildRustLib \
      -Drust.libraryPath="$(pwd)/core/rust/target/debug/libjava_bindings.so"
}

EJB_RUST_DIR="${PWD}/core/rust"

# Run all tests before packaging the App. This is safe, but takes a long time.
if [ "$#" -eq 0 ]; then
  ./run_all_tests.sh
else
  if [ "$1" != "--skip-tests" ]; then
    echo "Unknown option: $1"
    exit 1
  fi
  source ./tests_profile
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
