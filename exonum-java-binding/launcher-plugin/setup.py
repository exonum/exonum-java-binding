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

install_requires = ["exonum-launcher"]

python_requires = ">=3.6"

setup(
    name="exonum_java_plugins",
    version="0.9.0-SNAPSHOT",
    description="Exonum Java plugins for exonum_launcher",
    url="https://github.com/exonum/exonum-java-binding",
    packages=["exonum_java_runtime_plugin", "exonum_java_instance_plugin"],
    install_requires=install_requires,
    python_requires=python_requires,
)
