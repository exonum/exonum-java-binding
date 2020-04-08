# Java Plugins for [Exonum Launcher](https://github.com/exonum/exonum-launcher)

- Java Runtime Plugin allows to deploy and start Java services.
- Instance Configuration Plugin allows to use an arbitrary Protobuf message for service 
  initial configuration parameters. The plugin can be used for both Java and Rust services.

## Installation

### Requirements

- [Python3](https://www.python.org/downloads/)

### Using PyPI

```bash
pip3 install exonum-launcher-java-plugins
```

### From Source

```bash
# Generate sources
source ./tests_profile
mvn generate-sources -pl core
# Install plugins
python3 -m pip install -e exonum_launcher_java_plugins
```

## Usage

See [Deploy and Start the Service][deploy-and-start].

[deploy-and-start]: https://exonum.com/doc/version/1.0.0/get-started/java-binding/#deploy-and-start-the-service

# License

Apache License version 2.0. See [LICENSE](LICENSE) for details.
