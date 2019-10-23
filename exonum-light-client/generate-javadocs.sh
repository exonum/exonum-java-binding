#!/usr/bin/env bash
# Builds an archive with Javadocs from published artifacts.
# The archive is put in './target/site'

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Generate the aggregated Javadocs for the published modules
mvn clean javadoc:javadoc -Dmaven.javadoc.skip=false

# Create an archive to be published
TARGET="${PWD}/target/site/"
cd "${TARGET}"

ARCHIVE_NAME="light-client-apidocs.tgz"
tar cvaf "${ARCHIVE_NAME}" "apidocs/"

echo "[INFO] Javadoc archive created in ${TARGET}/${ARCHIVE_NAME}"
