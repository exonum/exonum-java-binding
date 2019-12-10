# Java Plugins for [Exonum Launcher](https://github.com/exonum/exonum-launcher)

- Java Runtime Plugin allows to deploy and start Java services.
- Instance Configuration Plugin allows to use an arbitrary Protobuf message for service 
  initial configuration parameters. The plugin can be used for both Java and Rust services.

## Installation

#### Requirements

- [Python3](https://www.python.org/downloads/)

```bash
# Generate sources
source ./tests_profile
mvn generate-sources -pl core
# Install plugins
python3 -m pip install -e exonum_launcher_java_plugins
```

## Usage

TODO: Move the entire section to the EJB App tutorial.

To deploy and start a specific list of services, use the following command with the
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

You need to specify every node for which you have an access to its private API port. If you
do not have an access to every node in the network, the administrators of other nodes must
run `exonum-launcher` with the same configuration file (with a different list of available nodes).

Deadline height describes the maximum blockchain height for the deployment process. Make sure to
specify the value larger than the current blockchain height.

```yaml
deadline_height: 20000
```

Enable Java runtime by specifying its identifier (`1`). Rust runtime is enabled by default:

```yaml
runtimes:
  java: 1
```

Add artifacts you want to deploy. For each artifact, you need to specify its name alias
(as YAML key) and its runtime (using `runtime` field). Name aliases are used in other parts
of configuration for readability and easier refactoring. Java artifacts also need name of the
`jar` file in the `spec: artifact_filename` field. In our example we add the Java
`cryptocurrency-demo` service, and two Rust services - the `timestamping` and `time` oracle services.

```yaml
artifacts:
  cryptocurrency:
    runtime: java
    name: "com.exonum.examples:cryptocurrency:0.9.0-rc1"
    spec:
      artifact_filename: "exonum-java-binding-cryptocurrency-demo-0.9.0-rc1-artifact.jar"
  time:
    runtime: rust
    name: "exonum-time:0.12.0"
  timestamping:
    runtime: rust
    name: "exonum-timestamping:0.12.0"
```

Add a `plugins` section to enable both Java Runtime plugin and Instance Configuration plugin.
Runtime plugin is enabled for a specific runtime (`java` in our example), while Instance
Configuration plugin is enabled for a specific artifact name alias (`timestamping` in our example).

```yaml
plugins:
  runtime:
    java: "exonum_java_runtime_plugin.JavaDeploySpecLoader"
  artifact:
    timestamping: "exonum_instance_configuration_plugin.InstanceSpecLoader"
```

In our example we will use the Instance Configuration plugin to serialize initial configuration parameters of
the `timestamping` service in Protobuf. We need to take a `service.proto` file with the message
description from the service sources and place it inside some known directory.

  ```proto
  syntax = "proto3";
  
  package exonum.examples.timestamping;
  
  message Config {
      string time_service_name = 1;
  }
  ```

Finally, add an `instances` section that describes the list of service instances you want to
start in the blockchain. For each instance you need to specify its artifact name alias.
Instance Configuration plugin also requires a list of additional parameters, which we
provide for the `timestamping` instance:

- `sources`. Points to a directory with the Protobuf sources of the service configuration 
message. We use the `proto_sources` directory.
- `config_message_source`. A file name where the `message_name` message 
is located. In our example we use the `service.proto` file.
- `message_name`. A name of the Protobuf message used to represent the service configuration.
  Optional, defaults to `Config`.
- `data`. Your actual configuration in the format corresponding to the `message_name` message.

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

See [sample-config.yml](sample-config.yml) for the final state of the configuration file.

# License

Apache License version 2.0. See [LICENSE](LICENSE) for details.
