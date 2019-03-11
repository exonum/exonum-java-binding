#!/usr/bin/env bash

# Copy Java and native libraries necessary for running EJB App
# to the special directories in rust/target/debug

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

echo "Sending files to tests..."
JAVA_LIBS_TESTS_DIR="$EJB_RUST_DIR/target/debug/lib/java"
RUST_LIBS_TESTS_DIR="$EJB_RUST_DIR/target/debug/lib/native"
rm -rf "$JAVA_LIBS_TESTS_DIR"; mkdir -p "$JAVA_LIBS_TESTS_DIR"
rm -rf "$RUST_LIBS_TESTS_DIR"; mkdir -p "$RUST_LIBS_TESTS_DIR"

CORE_TXT="${PWD}/core/target/ejb-core-classpath.txt"
EJB_CLASSPATH="$(cat ${CORE_TXT})"

IFS=':' read -r -a CORE_DEPS <<< "$EJB_CLASSPATH"
for dependency in "${CORE_DEPS[@]}"
do
  cp "$dependency" "$JAVA_LIBS_TESTS_DIR"
done

cp "${PWD}"/core/target/exonum-java-binding-core-*-SNAPSHOT.jar "$JAVA_LIBS_TESTS_DIR"

cp "${RUST_LIB_DIR}"/libstd* "${RUST_LIBS_TESTS_DIR}"
cp $EJB_RUST_DIR/target/debug/libjava_bindings.* $RUST_LIBS_TESTS_DIR
