#!/usr/bin/env bash
# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

mvn generate-sources

CRYPTOCURRENCY_FULL_CLASSPATH="cryptocurrency-classpath.txt"

# Add EJB-Core classpath
cat exonum-java-binding-core/target/ejb-core-classpath.txt > \
  "${CRYPTOCURRENCY_FULL_CLASSPATH}"

# Add delimiter
echo -n ":" >> "${CRYPTOCURRENCY_FULL_CLASSPATH}"

# Add Cryptocurrency classpath
cat exonum-java-binding-cryptocurrency-demo/target/cryptocurrency-classpath.txt >> \
 "${CRYPTOCURRENCY_FULL_CLASSPATH}"
