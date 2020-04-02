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

import setuptools

INSTALL_REQUIRES = ["exonum-launcher==0.2.0"]

PYTHON_REQUIRES = ">=3.6"

with open("README.md", "r") as readme:
    LONG_DESCRIPTION = readme.read()

setuptools.setup(
    name="exonum_launcher_java_plugins",
    version="0.10.0",
    author="The Exonum team",
    author_email="contact@exonum.com",
    description="Exonum Java plugins for exonum-launcher",
    long_description=LONG_DESCRIPTION,
    long_description_content_type="text/markdown",
    url="https://github.com/exonum/exonum-java-binding",
    packages=[
        "exonum_java_runtime_plugin",
        "exonum_java_runtime_plugin.proto.exonum.java",
        "exonum_instance_configuration_plugin",
        "exonum_instance_configuration_plugin.proto.exonum.java",
    ],
    install_requires=INSTALL_REQUIRES,
    python_requires=PYTHON_REQUIRES,
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
        "Topic :: Security :: Cryptography",
    ],
)
