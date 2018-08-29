# Exonum Java Binding App Tutorial
This document describes how to configure and run an Exonum node with a Java service using Java Binding App.

## Prerequisites

Build an application following the instructions in [“How to Build”][how-to-build] section
of the Contribution Guide.

You should also have a ready-to-use Exonum Java service with prepared ServiceModule class.
See [Java binding documentation](https://exonum.com/doc/get-started/java-binding/).

[how-to-build]: https://github.com/exonum/exonum-java-binding/blob/master/CONTRIBUTING.md#how-to-build

## How to Run an Exonum Node

### Step 1. Configure Environment

`EJB_ROOT` used in examples of this section corresponds to the Java Binding root directory.

#### `LD_LIBRARY_PATH`

`LD_LIBRARY_PATH` is required to locate native libraries used by Java Binding.
You need to provide path to JVM library (e.g. `libjvm.so` on Linux).

You can use the following script for this purpose:

```bash
JAVA_HOME="${JAVA_HOME:-$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')}"
LIBJVM_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"

export LD_LIBRARY_PATH="${LIBJVM_PATH}"
```

#### CLASSPATH
Classpath is used to locate Java classes of your service and its dependencies, including 
the Exonum Java Binding.

You may package your service in an Uber JAR using 
the [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/index.html)
and pass a path to the service artefact during application configuration as `--ejb-service-classpath`
parameter. Alternatively, you may assemble a classpath that includes the service and all of 
its dependencies and pass it instead.

### Step 2. Generate Node Configuration

EJB App configuration is pretty similar to configuration of any other Exonum service,
with a few additional parameters.

#### Generate Template Config
Use `--ejb-module-name` for fully qualified name of your service module.

```$sh
$ ejb-app generate-template testnet/common.toml \
    --validators-count=1
    --ejb-module-name 'com.company.project.ServiceModule'
```

#### Generate Node Private and Public Configs

```$sh
$ ejb-app generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
    --peer-address 127.0.0.1:5400
```

#### Finalize Configuration

Use `--ejb-classpath` for a [classpath](#CLASSPATH) of your service.

```$sh
$ ejb-app finalize testnet/sec.toml testnet/node.toml \
    --ejb-service-classpath $CLASSPATH \
    --public-configs testnet/pub.toml
```

### Step 3. Run Configured Node
There are two specific parameters here:
- `--ejb-log-config-path` for path to `log4j` configuration file.
  Default config provided with EJB App prints to STDOUT.
- `--ejb-port` for port that your service will use for communication.
  Java Binding does not use Exonum Core API port directly.

```$sh
$ ejb-app run -d testnet/db -c testnet/node.toml \
    --ejb-log-config-path "log4j.xml" \
    --ejb-port 6000 \
    --public-api-address 127.0.0.1:3000
```
