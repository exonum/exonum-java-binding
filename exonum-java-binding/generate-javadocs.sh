#!/usr/bin/env bash
# Builds an archive with Javadocs from published artifacts.
# The archive is put in './target/site'

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Build the Javadocs
mvn clean javadoc:aggregate -Dmaven.javadoc.skip=false \
  `# Include only the published artifacts. As package filtering wildcards are rather limited,\
   it is more convenient to specify the list of projects:` \
  --projects com.exonum.binding:exonum-java-binding-parent,common,core,testkit,time-oracle

# Create an archive to be published
TARGET="${PWD}/target/site/"
cd "${TARGET}"

ARCHIVE_NAME="java-binding-apidocs.tgz"
tar cvaf ${ARCHIVE_NAME} "apidocs/"

echo "[INFO] Javadoc archive created in ${TARGET}/${ARCHIVE_NAME}"
