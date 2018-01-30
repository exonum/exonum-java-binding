#!/usr/bin/env bash
# Runs native integration tests (those in ejb-core/rust/integration_tests)
# ¡Keep it MacOS/Ubuntu compatible!

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Use an already set JAVA_HOME, or infer it from java.home system property.
#
# Unfortunately, a simple `which java` will not work for some users (e.g., jenv),
# hence this a bit complex thing.
JAVA_HOME="${JAVA_HOME:-$(mvn --version | grep 'Java home' | sed 's/.*: //')}"
echo "JAVA_HOME=${JAVA_HOME}"

# Find the directory containing libjvm (the relative path has changed in Java 9)
export LD_LIBRARY_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"

# Compile all Java modules to ensure that ejb-fakes, which is required by native ITs,
# is up-to-date. This safety net takes about a dozen seconds. If that annoys you, please add
# an optional parameter that disables this command.
mvn -DskipTests compile --quiet

cd exonum-java-binding-core/rust

# Stable works well unless you want benchmarks.
RUST_COMPILER_VERSION="stable"

cargo "+${RUST_COMPILER_VERSION}" test \
  --manifest-path integration_tests/Cargo.toml
