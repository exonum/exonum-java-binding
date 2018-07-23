#!/usr/bin/env bash
# Stops the build if only markdown files were updated.
# If TRAVIS_COMMIT_RANGE env variable is not set, does nothing.

if [ -z "${TRAVIS_COMMIT_RANGE+x}" ];
then
  echo "TRAVIS_COMMIT_RANGE is not set."
elif ! git diff --name-only "${TRAVIS_COMMIT_RANGE}" | grep -qvE '(.md$)'
then
  echo "Only docs were updated, not running the CI: commit_range=${TRAVIS_COMMIT_RANGE}"
  exit
fi