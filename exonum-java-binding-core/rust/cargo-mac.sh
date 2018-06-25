#!/usr/bin/env bash
# Runs cargo commands with a prepared environment.
# Every native test requires loading libjvm.dylib because of using `invocation` feature of jni-rs.
# This script includes a path to libjvm.dylib into LD_LIBRARY_PATH and should be used instead of a regular cargo
# executable while working with EJB on Mac.
#
# ¡Keep it MacOS compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Use Java that Maven uses.
#
# Unfortunately, a simple `which java` will not work for some users (e.g., jenv),
# hence this a bit complex thing.
JAVA_HOME="$(mvn --version | grep 'Java home' | sed 's/.*: //')"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9)
export LD_LIBRARY_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

echo
echo "cargo $@"
echo
cargo $@
