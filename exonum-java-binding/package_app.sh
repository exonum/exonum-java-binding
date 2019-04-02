#!/usr/bin/env bash
# Package EJB App after preparing necessary environment variables and file structure.
#
# ¡Keep it MacOS/Ubuntu compatible!

# This script runs all tests, builds and packages the EJB App into a single zip archive with all
# necessary dependencies. The workflow is the following:
# 1. Run all tests
# 2. Prepare special directories inside `core/rust/target/debug` directory:
#    - etc/ - for licenses, TUTORIAL.md and fallback logger configuration
#    - lib/native/ - for native dynamic libraries used by the App
#    - lib/java/ - for Java classes used by the App
# 3. Copy available files to corresponding directories performed at step 2
# 4. Compile the App and package it using special Maven module `packaging` with `app-packaging` profile
#    At this step, we copy the files from prepared at step 2 directories.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

function build-ejb-app() {
    mvn package --activate-profiles ${BUILD_PROFILE} -pl :exonum-java-binding-packaging -am \
      -DskipTests \
      -Dbuild.mode=${BUILD_MODE} \
      -DskipJavaITs \
      -DdoNotBuildRustLib \
      -Drust.libraryPath="${RUST_LIBRARY_PATH}"
}

function build-ejb-app-macos() {
    export RUSTFLAGS="-C link-arg=-Wl,-rpath,@executable_path/lib/native"
    echo "Setting new RUSTFLAGS=${RUSTFLAGS}"
    export RUST_LIBRARY_PATH="${PACKAGING_BASE_DIR}/libjava_bindings.dylib"
    echo "RUST_LIBRARY_PATH=${RUST_LIBRARY_PATH}"
    build-ejb-app
}

function build-ejb-app-linux() {
    export RUSTFLAGS="-C link-arg=-Wl,-rpath,\$ORIGIN/lib/native/"
    echo "Setting new RUSTFLAGS=${RUSTFLAGS}"
    export RUST_LIBRARY_PATH="${PACKAGING_BASE_DIR}/libjava_bindings.so"
    build-ejb-app
}

EJB_RUST_DIR="${PWD}/core/rust"

SKIP_TESTS=false
BUILD_MODE=debug
# Can be either `package-app` or `package-app-release`
BUILD_PROFILE=package-app

while (( "$#" )); do
  case "$1" in
    --skip-tests)
      SKIP_TESTS=true
      shift 1
      ;;
    --release)
      BUILD_MODE=release
      BUILD_PROFILE=package-app-release
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
cp ./core/rust/ejb-app/log4j-fallback.xml "${PACKAGING_ETC_DIR}"

# Copy tutorial
cp ./core/rust/ejb-app/TUTORIAL.md "${PACKAGING_ETC_DIR}"

if [[ "$(uname)" == "Darwin" ]]; then
    build-ejb-app-macos
elif [[ "$(uname -s)" == Linux* ]]; then
    build-ejb-app-linux
fi
