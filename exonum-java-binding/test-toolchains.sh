#!/usr/bin/env bash

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

function test_compile() {
  # Local (Maven) VM
  local LOCAL_VM=$1
  # Target branch
  local BRANCH=$2
  local NAME="${BRANCH} / ${LOCAL_VM}"

  echo "[INFO] Start '${NAME}'"
  jenv local "${LOCAL_VM}"
  git checkout "${BRANCH}"
  source tests_profile
  mvn clean -q
  time mvn test-compile --threads=1 -DskipRustLibBuild -Dprofile="${NAME}"
  echo "[INFO] See time above for '${NAME}'"
}

test_compile "11.0" "java-11-base"
#test_compile "13.0" "java-11-base"
#test_compile "1.8" "java-11-base"

test_compile "11.0" "java-11"
# test_compile "13.0", "java-11"
