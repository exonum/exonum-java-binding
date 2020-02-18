#!/usr/bin/env bash
# Runs unit tests of exonum_launcher_java_plugins.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Echo commands so that the progress can be seen in CI server logs.
set -x

cd "${TRAVIS_BUILD_DIR}/exonum-java-binding"

# Generate protobuf files needed for plugins
source ./tests_profile
mvn install -DskipTests -DskipRustLibBuild -pl common -am

cd "exonum_launcher_java_plugins"

# Install pip
curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
python3.7 get-pip.py --user

# Install dependencies
pip3 install --user -r requirements.txt --no-binary=protobuf

# Install exonum_launcher_java_plugins
pip3 install --user -e .

# Download latest protobuf compiler
wget https://github.com/protocolbuffers/protobuf/releases/download/v3.11.3/protoc-3.11.3-linux-x86_64.zip
unzip protoc-3.11.3-linux-x86_64.zip
export PROTOC="$(pwd)/bin/protoc"

# Run tests
cd tests
python3.7 -m unittest -v
