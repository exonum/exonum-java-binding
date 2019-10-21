# Java Runtime Plugin for Exonum Launcher

## Installation

#### Requirements

- [Python3](https://www.python.org/downloads/)
- [`exonum-python-client`](https://github.com/exonum/exonum-python-client)
- [`exonum-launcher`](https://github.com/exonum/exonum-launcher)

```bash
cd exonum-java-binding
python3 -m pip install -e launcher-plugin
```

## Usage

TODO: Move the entire section to the EJB App tutorial.

Add `plugins` session to configuration file of the Exonum Launcher:

```yaml
runtimes:
  java: 1

plugins:
  runtime:
    java: "exonum_java_runtime_plugin.JavaDeploySpecLoader"
  artifact: {}
```

To load service artifact, provide a filename of the service artifact, for example:

```yaml
artifacts:
  cryptocurrency:
    runtime: java
    name: "com.exonum.examples:cryptocurrency:0.9.0-SNAPSHOT"
    spec:
      artifact_filename: "cryptocurrency-0.9.0-SNAPSHOT-artifact.jar"
```

# License

Apache License version 2.0. See [LICENSE](LICENSE) for details.
