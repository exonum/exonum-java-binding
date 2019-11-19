# Java Runtime Plugins for [Exonum Launcher](https://github.com/exonum/exonum-launcher)

- Java Runtime Plugin allows to deploy and start Java services.
- Java Instance Plugin allows to use an arbitrary Protobuf message for service 
  initial configuration parameters. The plugin can be used for both Java and Rust services.

## Installation

#### Requirements

- [Python3](https://www.python.org/downloads/)

```bash
# Generate sources
mvn generate-sources -pl core
# Install plugins
python3 -m pip install -e launcher-plugins
```

## Usage

TODO: Move the entire section to the EJB App tutorial.

To deploy and start a specific list of services, use the following command with a
prepared `config.yml` file:

```bash
python3 -m exonum_launcher -i config.yml
```

### Writing Configuration File

Start with specifying IP addresses of the blockchain nodes:

```yaml
networks:
  - host: "127.0.0.1"
    ssl: false
    public-api-port: 8080
    private-api-port: 8081
```

You need to specify every node in the network.

Deadline height describes the maximum blockchain height for deployment process. Make sure to
specify the value larger than current blockchain height.

```yaml
deadline_height: 20000
```

Enable Java runtime by specifying its identifier (`1`). Rust runtime is enabled by default:

```yaml
runtimes:
  java: 1
```

Add artifacts to be deployed. For each artifact, you need to specify its name alias (as YAML key) 
and its runtime (using `runtime` field). Name aliases are used in other parts of configuration
for readability and easier refactoring. Java artifacts also need name of the `jar` file in 
`spec: artifact_filename` field. In our example we add Java `cryptocurrency-demo` service, and two
Rust services - `timestamping` and `time` oracle service.

```yaml
artifacts:
  cryptocurrency:
    runtime: java
    name: "com.exonum.examples:cryptocurrency:0.9.0-SNAPSHOT"
    spec:
      artifact_filename: "exonum-java-binding-cryptocurrency-demo-0.9.0-SNAPSHOT-artifact.jar"
  time:
    runtime: rust
    name: "exonum-time:0.12.0"
  timestamping:
    runtime: rust
    name: "exonum-timestamping:0.12.0"
```

Add `plugins` section to enable both Java Runtime plugin and Java Instance plugin. Runtime plugin is
enabled for a specific runtime (`java` in our example), while Instance plugin is enabled for a
specific artifact name alias (`timestamping` in our example).

```yaml
plugins:
  runtime:
    java: "exonum_java_runtime_plugin.JavaDeploySpecLoader"
  artifact:
    timestamping: "exonum_java_instance_plugin.JavaInstanceSpecLoader"
```

In our example we will use Java Instance plugin to serialize initial configuration parameters of
`timestamping` service in Protobuf. We need to take a `service.proto` file with message
description from service sources and place it inside some known directory.

  ```proto
  syntax = "proto3";
  
  package exonum.examples.timestamping;
  
  message Config {
      string time_service_name = 1;
  }
  ```

Finally, add `instances` section that describes the list of service instances to be started in the
blockchain. For each instance you need to specify artifact name alias. Java Instance plugin also
requires a list of additional parameters, which we provide for `timestamping` instance:

- `sources`. Points to a directory with Protobuf-sources of service configuration 
message. We use `proto_sources` directory.
- `config_message_source`. A file name where `message_name` message 
is located. In our example we use `service.proto` file.
- `message_name`. A name of the Protobuf message used to represent service configuration.
  Optional, defaults to `Config`.
- `data`. Your actual configuration in the format corresponding to `message_name` message.

```yaml
instances:
  cryptocurrency:
    artifact: cryptocurrency
  time:
    artifact: time
  timestamping:
    artifact: timestamping
    config:
      sources: "proto_sources"
      config_message_source: "service.proto"
      message_name: "Config"
      data:
        time_service_name: "time"
```

See [sample-config.yml](sample-config.yml) for the final state of configuration file.

# License

Apache License version 2.0. See [LICENSE](LICENSE) for details.
