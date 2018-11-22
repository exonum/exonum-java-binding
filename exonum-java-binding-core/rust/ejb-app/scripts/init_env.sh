#!/usr/bin/env bash

# Fixes tha lack of the `realpath` tool in OS X.
if [ ! $(which realpath) ]; then
    function realpath() {
        python -c 'import os, sys; print os.path.realpath(sys.argv[1])' "${1%}"
    }
fi

function header() {
    local title=$1
    local rest="========================================================================"
    echo
    echo "===[ ${title} ]${rest:${#title}}"
    echo
}

header "INIT ENVIRONMENT"

header "Java environment"
export JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')"
echo "JAVA_HOME=${JAVA_HOME}"
export JVM_LIB_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
echo "JVM_LIB_PATH=${JVM_LIB_PATH}"

header "Rust environment"
#export RUST_COMPILER_VERSION="${RUST_COMPILER_VERSION:-1.27.2}"
#echo "RUST_COMPILER_VERSION: ${RUST_COMPILER_VERSION}"
#export RUST_LIB_DIR=$(rustup run ${RUST_COMPILER_VERSION} rustc --print sysroot)/lib
#echo "RUST_LIB_DIR: ${RUST_LIB_DIR}"

RUST_LIB_DIR="$(rustup run 1.27.2 rustc --print sysroot)/lib"
echo "RUST_LIB_DIR=${RUST_LIB_DIR}"

header "Project paths"

export EJB_ROOT=$(realpath "../../../..")
echo "PROJECT_ROOT=${EJB_ROOT}"
export EJB_APP=$(realpath "..")
echo "EJB_APP=${EJB_APP}"


export EJB_NATIVE_LIB_DIR="${EJB_ROOT}/exonum-java-binding-core/rust/target/debug"
export LD_LIBRARY_PATH="$EJB_NATIVE_LIB_DIR:$RUST_LIB_DIR:$JVM_LIB_PATH"
