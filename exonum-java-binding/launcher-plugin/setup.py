#!/usr/bin/env python
from distutils.core import setup

install_requires = []  # Since `exonum-launcher` is not on pypi yet, suppose we have it pre-installed.

python_requires = ">=3.6"

setup(
    name="exonum_java_runtime_plugin",
    version="0.1",
    description="Exonum Java runtime plugin for exonum_launcher",
    url="https://github.com/exonum/exonum-java-binding",
    packages=["exonum_java_runtime_plugin"],
    install_requires=install_requires,
    python_requires=python_requires,
)
