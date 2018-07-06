#!/usr/bin/env bash
# Runs EJB App tests (ejb-core/rust/ejb-app).
# If --skip-compile is passed, does not recompile all Java sources.
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Use the Java that Maven uses.
#
# Unfortunately, a simple `which java` will not work for some users (e.g., jenv),
# hence this a bit complex thing.
JAVA_HOME="$(mvn --version | grep 'Java home' | sed 's/.*: //')"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9)
export LD_LIBRARY_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

cd exonum-java-binding-core/rust

# Stable works well unless you want benchmarks.
RUST_COMPILER_VERSION="stable"

cargo "+${RUST_COMPILER_VERSION}" test \
  --manifest-path ejb-app/Cargo.toml
