#!/usr/bin/env bash
# Runs native integration tests (those in ejb-core/rust/integration_tests).
# If --skip-compile is passed, does not recompile all Java sources.
#
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Use the Java that Maven uses.
#
# Unfortunately, a simple `which java` will not work for some users (e.g., jenv),
# hence this a bit complex thing.
JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9)
export LD_LIBRARY_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

# Compile all Java modules by default to ensure that ejb-fakes module, which is required
# by native ITs, is up-to-date. This safety net takes about a dozen seconds,
# so if the Java artefacts are definitely up-to-date, it may be skipped.
if [ "$#" -eq 0 ]; then
  # Compile Java artefacts.
  echo "Compiling the Java artefacts…"
  mvn -DskipTests compile --quiet
else
  if [ "$1" != "--skip-compile" ]; then
    echo "Unknown option: $1"
    exit 1
  fi
fi

cd exonum-java-binding-core/rust

## Stable works well unless you want benchmarks.
# TODO: stable does not work well until ECR-1839 is resolved
RUST_COMPILER_VERSION="${RUST_VERSION:-1.26.2}"

cargo "+${RUST_COMPILER_VERSION}" test
