# Java Runtime Plugins for Exonum Launcher

## Installation

#### Requirements

- [Python3](https://www.python.org/downloads/)

```bash
# Generate sources
mvn generate-sources -pl core
# Install plugin
python3 -m pip install -e launcher-plugins
```

## Usage

TODO: Move the entire section to the EJB App tutorial.

### Java Runtime Plugin

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

### Java Instance Plugin

Add `plugins` session to configuration file of the Exonum Launcher:

```yaml
runtimes:
  java: 1

plugins:
  runtime: {}
  artifact: 
    service_artifact_name: "exonum_java_instance_plugin.JavaInstanceSpecLoader"
```

To instantiate a service with a custom configuration you need to take a Protobuf
source of the configuration message and place it in specific directory:

  ```proto
  syntax = "proto3";
  
  package exonum.examples.service_name;
  
  message ServiceConfig {
      string time_service_name = 1;
  }
  ```

To instantiate a service, add the following fields to the instance `config`:

- `sources`. Points to a directory with Protobuf-sources of service configuration 
message. We use `proto_sources` directory.
- `module_name`. A name (without extension) of the file where `message_name` message 
is located. In our example we use `service.proto` file.
- `message_name`. A name of the Protobuf message used to represent service configuration.
  Optional, defaults to `Config`.
- `data`. Your actual configuration in the format corresponding to `message_name` message.

```yaml
instances:
  service_instance_name:
    artifact: service_artifact_name
    config:
      sources: "proto_sources"
      module_name: "service"
      message_name: "ServiceConfig"
      data:
        time_service_name: "testing"
```

# License

Apache License version 2.0. See [LICENSE](LICENSE) for details.
