#!/usr/bin/env bash
# Package Exonum Java app after preparing necessary environment variables and file structure.
#
# ¡Keep it MacOS/Ubuntu compatible!

# This script runs all tests, builds and packages the Exonum Java app into a single zip archive with all
# necessary dependencies. The workflow is the following:
# 1. Run all tests
# 2. Prepare special directories inside `core/rust/target/${BUILD_MODE}` directory:
#    - etc/ - for licenses, TUTORIAL.md and fallback logger configuration
#    - lib/native/ - for native dynamic libraries used by the App
#    - lib/java/ - for Java classes used by the App
# 3. Copy available files to corresponding directories performed at step 2
# 4. Compile the App and package it using special Maven module `packaging` with `package-app` profile
#    At this step, we copy the files from prepared at step 2 directories.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

function build-exonum-java() {
    mvn package --activate-profiles package-app -pl :exonum-java-binding-packaging -am \
      -DskipTests \
      -Dbuild.mode=${BUILD_MODE} \
      -Dbuild.cargoFlag=${BUILD_CARGO_FLAG} \
      -DdoNotBuildRustLib \
      -Drust.libraryPath="${RUST_LIBRARY_PATH}"
}

# Prepares native environment for building with build-exonum-java function. The first argument of function is platform
# dependent rpath value for linker that points to the directory where code is executed/loaded. For currently supported
# architectures they are "@loader_path" for OSX and "$ORIGIN" for Linux. The second argument is full name of the
# java_binding library for current platform.
function build-exonum-java-for-platform() {
    # This will point to the directory with native libraries in case code was executed by "exonum-java"
    local path_lib_from_exe="$1/lib/native"
    # This will point to the directory with native libraries in case code was loaded by java
    local path_lib_from_lib=$1
    local full_lib_name=$2

    export RUSTFLAGS="-C link-arg=-Wl,-rpath,${path_lib_from_exe} -C link-arg=-Wl,-rpath,${path_lib_from_lib}"
    echo "Setting new RUSTFLAGS=${RUSTFLAGS}"
    export RUST_LIBRARY_PATH="${PACKAGING_BASE_DIR}/${full_lib_name}"
    build-exonum-java
}

function build-exonum-java-macos() {
    build-exonum-java-for-platform "@loader_path" "libjava_bindings.dylib"
}

function build-exonum-java-linux() {
    build-exonum-java-for-platform "\$ORIGIN" "libjava_bindings.so"
}

EJB_RUST_DIR="${PWD}/core/rust"

# Run tests by default. To skip, use `--skip-tests` flag
SKIP_TESTS=false
# Debug mode by default. To switch to release, use `--release` flag
BUILD_MODE=debug
# This var is used by maven to run `cargo` with no extra flags in case of debug builds and with `--release` flag for
#   release builds
BUILD_CARGO_FLAG=""

while (( "$#" )); do
  case "$1" in
    --skip-tests)
      SKIP_TESTS=true
      shift 1
      ;;
    --release)
      BUILD_MODE=release
      BUILD_CARGO_FLAG="--release"
      shift 1
      ;;
    *) # anything else
      echo "Usage: package_app.sh [--skip-tests] [--release]" >&2
      exit 1
      ;;
  esac
done

if [ $SKIP_TESTS != true ]; then
  ./run_all_tests.sh
fi
source ./tests_profile

# Prepare directories
PACKAGING_BASE_DIR="${EJB_RUST_DIR}/target/${BUILD_MODE}"
PACKAGING_NATIVE_LIB_DIR="${PACKAGING_BASE_DIR}/lib/native"
PACKAGING_ETC_DIR="${PACKAGING_BASE_DIR}/etc"
mkdir -p "${PACKAGING_BASE_DIR}"
mkdir -p "${PACKAGING_NATIVE_LIB_DIR}"
mkdir -p "${PACKAGING_ETC_DIR}"

# Copy libstd to some known place.
cp ${RUST_LIB_DIR}/libstd* "${PACKAGING_NATIVE_LIB_DIR}"

# Copy licenses so that the package tool can pick them up.
cp ../LICENSE "${PACKAGING_ETC_DIR}"
cp LICENSES-THIRD-PARTY.TXT "${PACKAGING_ETC_DIR}"

# Generate licenses for native dependencies.
./core/rust/generate_licenses.sh

# Copy fallback logger configuration
cp ./core/rust/exonum-java/log4j-fallback.xml "${PACKAGING_ETC_DIR}"

# Copy tutorial
cp ./core/rust/exonum-java/TUTORIAL.md "${PACKAGING_ETC_DIR}"

if [[ "$(uname)" == "Darwin" ]]; then
    build-exonum-java-macos
elif [[ "$(uname -s)" == Linux* ]]; then
    build-exonum-java-linux
fi
