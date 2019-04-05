# Exonum Java Binding App Tutorial
This document describes how to configure and run an Exonum node with a Java service using Java Binding App.

## Prerequisites

Build an application following the instructions in [“How to Build”][how-to-build] section
of the Contribution Guide. Unpack the zip archive from `exonum-java-binding/packaging/target` directory to some known 
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

#### Services definition
Services must be defined in the [ejb_app_services.toml](https://exonum.com/doc/version/0.10/get-started/java-binding/#built-in-services) 
file in order to be available in the network. The configuration file consists of two sections:
- The optional `system_services` section is used to enable built-in Exonum services. If 
not specified - only Configuration service is enabled. Possible variants for the moment are: 
`configuration`, `btc-anchoring`, `time`.
- The `user_services` section is used to enumerate user services that are loaded from artifacts 
in the JAR format. It takes a line per service in form of `name = artifact`, where `name` 
is one-word description of the service and `artifact` is a full path to the service's artifact. 
At least one service must be defined.

The sample of `ejb_app_services.toml` file that enables all possible built-in Exonum services 
and two user services:
```toml
system_services = ["configuration", "btc-anchoring", "time"]

[user_services]
service_name1 = "/path/to/service1_artifact.jar"
service_name2 = "/path/to/service2_artifact.jar"
```

### Step 2. Generate Node Configuration

EJB App configuration is pretty similar to configuration of any other Exonum service,
with a few additional parameters.

#### Generate Template Config

```$sh
$ ejb-app generate-template testnet/common.toml \
    --validators-count=1
```

#### Generate Node Private and Public Configs

```$sh
$ ejb-app generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
    --peer-address 127.0.0.1:5400
```

#### Finalize Configuration

```$sh
$ ejb-app finalize testnet/sec.toml testnet/node.toml \
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
