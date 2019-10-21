#!/usr/bin/env python

# Copyright 2019 The Exonum Team
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from distutils.core import setup
from distutils.command.build_py import build_py
from distutils.command.clean import clean
from distutils.spawn import find_executable
from distutils.debug import DEBUG

import os
import sys
import subprocess

protobuf_source = "exonum_java_runtime_plugin/proto/service_runtime.proto"

if 'PROTOC' in os.environ and os.path.exists(os.environ['PROTOC']):
    protoc = os.environ['PROTOC']
else:
    protoc = find_executable('protoc')


def generate_proto(source):
    """Invoke Protocol Compiler to generate python from given source .proto."""
    if not os.path.exists(source):
        sys.stderr.write('Can\'t find required file: %s\n' % source)
        sys.exit(1)

    output = source.replace('.proto', '_pb2.py')
    if (not os.path.exists(output) or
            (os.path.getmtime(source) > os.path.getmtime(output))):
        if DEBUG:
            print('Generating %s' % output)

        if protoc is None:
            sys.stderr.write(
                'protoc not found. Is protobuf-compiler installed? \n'
                'Alternatively, you can point the PROTOC environment variable at a '
                'local version.')
            sys.exit(1)

        protoc_command = [protoc, '-I.', '--python_out=.', source]
        if subprocess.call(protoc_command) != 0:
            sys.exit(1)


class BuildPy(build_py):
    def run(self):
        generate_proto(protobuf_source)
        build_py.run(self)


class Clean(clean):
    def run(self):
        # Delete generated files in the code tree.
        for (dirpath, dirnames, filenames) in os.walk("."):
            for filename in filenames:
                filepath = os.path.join(dirpath, filename)
                if filepath.endswith("_pb2.py"):
                    os.remove(filepath)
        clean.run(self)


install_requires = []  # Since `exonum-launcher` is not on pypi yet, suppose we have it pre-installed.

python_requires = ">=3.6"

setup(
    name="exonum_java_runtime_plugin",
    version="0.9.0-SNAPSHOT",
    description="Exonum Java runtime plugin for exonum_launcher",
    url="https://github.com/exonum/exonum-java-binding",
    packages=["exonum_java_runtime_plugin"],
    install_requires=install_requires,
    python_requires=python_requires,
    cmdclass={
        'build_py': BuildPy,
        'clean': Clean,
    }
)
