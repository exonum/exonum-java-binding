# Exonum Java Binding App Tutorial
This document describes how to configure and run an Exonum node with a Java service using Java Binding App.

## Prerequisites

Build an application following the instructions in [“How to Build”][how-to-build] section
of the Contribution Guide. Unpack the zip archive from `exonum-java-binding/core/target` directory to some known 
location.

You also need a ready-to-use Exonum Java service. You can use 
[cryptocurrency-demo][cryptocurrency-demo] as an example, and find information about 
implementing your own Exonum service 
in the [user guide](https://exonum.com/doc/version/0.10/get-started/java-binding/).

[how-to-build]: https://github.com/exonum/exonum-java-binding/blob/master/CONTRIBUTING.md#how-to-build
[cryptocurrency-demo]: https://github.com/exonum/exonum-java-binding/tree/master/exonum-java-binding/cryptocurrency-demo

## How to Run an Exonum Node

### Step 1. Configure Environment

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
Classpath is used to locate Java classes of your service and its dependencies.

You may package your service in an Uber JAR using 
the [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/index.html)
and pass a path to the service artefact during application configuration as `--ejb-service-classpath`
parameter. Alternatively, you may assemble a classpath that includes the path to service and all of 
its dependencies and pass it instead.

The service must use `provided` scope for all Exonum dependencies because they are included in the application.

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

Use `--ejb-service-classpath` for a [classpath](#CLASSPATH) of your service.

```$sh
$ ejb-app finalize testnet/sec.toml testnet/node.toml \
    --ejb-service-classpath $CLASSPATH \
    --public-configs testnet/pub.toml
```

### Step 3. Run Configured Node
There are two required parameters here:
- `--ejb-log-config-path` for path to `log4j` configuration file.
  Default config `log4j-fallback.xml` provided with EJB App prints to STDOUT.
- `--ejb-port` for port that your service will use for communication.
  Java Binding does not use Exonum Core API port directly.

There are also optional parameters useful for debugging purposes and JVM fine tuning:
- `--jvm-args-prepend` and `--jvm-args-append`: Additional parameters for JVM that prepend and
 append the rest of arguments. Must not have a leading dash. For example, `Xmx2G`.
- `--jvm-debug`: Allows JVM being remotely debugged over the `JDWP` protocol. Takes a socket address as a parameter in form
 of `HOSTNAME:PORT`. For example, `localhost:8000`.
 
```$sh
$ ejb-app run -d testnet/db -c testnet/node.toml \
    --ejb-log-config-path "log4j.xml" \
    --ejb-port 6000 \
    --public-api-address 127.0.0.1:3000
```

#### Debugging the JVM

To enable remote debugging of Java code on a running Exonum node, 
pass `--jvm-debug` option with a socket address to connect to
from a debugger:

```sh
$ ejb-app run -d testnet/db -c testnet/node.toml --public-api-address 127.0.0.1:3000 \
    --ejb-log-config-path "log4j-fallback.xml" \
    --ejb-port 6000 \
    --jvm-debug localhost:8000
```

Now you can debug the service using any JDWP client, such as command line
JDB or a debugger built in your IDE:

```sh
$ jdb -attach localhost:8000 -sourcepath /path/to/source
```
