#!/usr/bin/env bash
# Runs cargo commands with a prepared environment.
# Every native test requires loading libjvm.{dylib|so} because of using `invocation` feature of jni-rs.
# This script includes path to libjvm to LD_LIBRARY_PATH and should be used instead of regular cargo executable while
# working with bindings.
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Use Java that Maven uses.
#
# Unfortunately, a simple `which java` will not work for some users (e.g., jenv),
# hence this a bit complex thing.
JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9)
export LD_LIBRARY_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

echo
echo "cargo $@"
echo
cargo $@
