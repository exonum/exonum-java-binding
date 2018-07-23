#!/usr/bin/env bash
# Stops the build if only markdown files were updated.
# TRAVIS_COMMIT_RANGE env variable must be set.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

if ! git diff --name-only "${TRAVIS_COMMIT_RANGE}" | grep -qvE '(.md$)'
then
  echo "Only docs were updated, not running the CI."
  exit
fi