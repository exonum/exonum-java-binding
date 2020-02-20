#!/usr/bin/env bash
# Builds an archive with Javadocs from published artifacts.
# The archive is put in './target/site'

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Import necessary environment variables (see the tests_profile header comment for details).
source tests_profile

# The published modules for which Javadocs must be generated
PROJECTS_TO_DOCUMENT="com.exonum.binding:exonum-java-binding-parent,messages,common,core,testkit,time-oracle"

# Clean the project and install the artifacts in the local repository,
# so that Javadocs can be generated for a subset of the modules — the published ones
mvn clean install -DskipTests -DskipRustLibBuild --projects "${PROJECTS_TO_DOCUMENT}" --also-make

# Generate the aggregated Javadocs for the published modules
mvn javadoc:aggregate -Dmaven.javadoc.skip=false -DskipRustLibBuild \
  `# Include *only* the published artifacts. As package filtering wildcards in Javadoc utility \
     are rather limited, it is more convenient to specify the list of projects:` \
  --projects "${PROJECTS_TO_DOCUMENT}"

# Create an archive to be published
TARGET="${PWD}/target/site/"
cd "${TARGET}"

ARCHIVE_NAME="java-binding-apidocs.zip"
zip -r ${ARCHIVE_NAME} "apidocs"

echo "[INFO] Javadoc archive created in ${TARGET}/${ARCHIVE_NAME}"
