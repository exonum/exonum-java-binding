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

## Purpose

- Java Runtime Plugin allows to deploy and start Java services.
- Java Instance Plugin allows to use an arbitrary Protobuf message for service 
  initial configuration parameters. The plugin can be used for both Java and Rust services.

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
  service_artifact_name:
    runtime: java
    name: "com.exonum.examples:service-name:0.9.0-SNAPSHOT"
    spec:
      artifact_filename: "service-artifact-filename.jar"
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
- `config_message_source`. A file name where `message_name` message 
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
      config_message_source: "service.proto"
      message_name: "ServiceConfig"
      data:
        time_service_name: "testing"
```

### Example

See [sample-config.yml](sample-config.yml) for an example of usage for both Runtime
and Instance plugins. The example configuration describes three services: Java
`cryptocurrency-demo`, Rust `timestamping` and `time` services. Java Instance plugin
is used to configure `timestamping` service, while Java Runtime plugin is used to
deploy and start `cryptocurrency-demo`.

# License

Apache License version 2.0. See [LICENSE](LICENSE) for details.
