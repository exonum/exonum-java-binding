#!/usr/bin/env bash
# Run tutorial tests in development mode (i.e., with a java_bindings library in
# core/target/debug). To run them with an installed application, simply invoke `mvn verify`.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Warn on set EXONUM_HOME
if [[ "${EXONUM_HOME:-}" != "" ]]; then
  echo "[WARNING] EXONUM_HOME is set and will be ignored: ${EXONUM_HOME}"
  echo "[WARNING] If you need to run the tests with an installed application, use mvn verify"
fi

# Check the native lib path exists
NATIVE_LIB_PATH="${PWD}/core/rust/target/debug"
if [[ ! -d "$NATIVE_LIB_PATH" ]]; then
  echo "[ERROR] The native library path does not exist: $NATIVE_LIB_PATH"
  echo "[ERROR] Check you run the script from EJB root. If that's correct, then build
the EJB first."
  exit 1
fi

# Run the tests
mvn verify -DnativeLibPath="$NATIVE_LIB_PATH" -f tutorials
