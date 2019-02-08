# Exonum Java Binding App Tutorial
This document describes how to configure and run an Exonum node with a Java service using Java Binding App.

## Prerequisites

Build an application following the instructions in [“How to Build”][how-to-build] section
of the Contribution Guide.

You also need a ready-to-use Exonum Java service. You can use 
[cryptocurrency-demo][cryptocurrency-demo] as an example, and find information about 
implementing your own Exonum service 
in the [user guide](https://exonum.com/doc/version/latest/get-started/java-binding/).

[how-to-build]: https://github.com/exonum/exonum-java-binding/blob/master/CONTRIBUTING.md#how-to-build
[cryptocurrency-demo]: https://github.com/exonum/exonum-java-binding/tree/master/exonum-java-binding/cryptocurrency-demo

## How to Run an Exonum Node

### Step 1. Configure Environment

`EJB_ROOT` used in examples of this section corresponds to the Java Binding root directory.

#### `LD_LIBRARY_PATH` and `EJB_LIBPATH`

`LD_LIBRARY_PATH` is required to locate native libraries used by Java Binding.
You need to provide paths to:
  - JVM library (e.g., `libjvm.so` on Linux).
  - Rust standard library that is used to build the application.
  - Application libraries used by Java Binding.

You can use the following script for this purpose:

```bash
JAVA_HOME="${JAVA_HOME:-$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')}"
LIBJVM_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"

RUST_LIB_PATH="$(rustup run stable rustc --print sysroot)/lib"

export EJB_LIBPATH="${EJB_ROOT}/core/rust/target/debug"

export LD_LIBRARY_PATH="${LIBJVM_PATH}:${RUST_LIB_PATH}:${EJB_LIBPATH}"
```

#### CLASSPATH
Classpath is used to locate Java classes of your service and its dependencies, including 
the Exonum Java Binding.

You may package your service in an Uber JAR using 
the [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/index.html)
and pass a path to the service artefact during application configuration as `--ejb-classpath`
parameter. Alternatively, you may assemble a classpath that includes the service and all of 
its dependencies and pass it instead.

### Step 2. Generate Node Configuration

EJB App configuration is pretty similar to configuration of any other Exonum service,
with a few additional parameters.

**Note:** using `cargo run` command requires working from the `ejb-app` directory.
This would be fixed in the future EJB versions.

#### Generate Template Config

```$sh
$ cargo run -- generate-template testnet/common.toml
```

#### Generate Node Private and Public Configs

- `--ejb-classpath` for a classpath of your service.
- `--ejb-libpath` for a path to Java bindings native libraries.

```$sh
$ cargo run -- generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
    --ejb-classpath $EJB_CLASSPATH \
    --ejb-libpath $EJB_LIBPATH \
    --peer-address 127.0.0.1:5400
```

#### Finalize Configuration

There are two specific parameters here:
- `--ejb-module-name` for fully qualified name of your `ServiceModule`.
- `--ejb-port` for port that your service will use for communication.
  Java Binding does not use Exonum Core ports directly.

```$sh
$ cargo run -- finalize testnet/sec.toml testnet/node.toml \
    --ejb-module-name 'com.<company-name>.<project-name>.ServiceModule' \
    --ejb-port 6000 \
    --public-configs testnet/pub.toml
```

### Step 3. Run Configured Node

```$sh
$ cargo run -- run -d testnet/db -c testnet/node.toml --public-api-address 127.0.0.1:3000
```

There are few specific optional parameters here:
- `--jvm-args-prepend` and `--jvm-args-append`: Additional parameters for JVM that prepend and
 append the rest of arguments. Must not have a leading dash. For example, `Xmx2G`.
- `--jvm-debug`: Allows JVM being remotely debugged over the `JDWP` protocol. Takes a socket address as a parameter in form
 of `HOSTNAME:PORT`. For example, `localhost:8000`.

#### Debugging the JVM

To enable remote debugging of Java code on a running Exonum node, 
pass `--jvm-debug` option with a socket address to connect to
from a debugger:

```sh
$ cargo run -- run -d testnet/db -c testnet/node.toml --public-api-address 127.0.0.1:3000 \
    --jvm-debug localhost:8000
```

Now you can debug the service using any JDWP client, such as command line
JDB or a debugger built in your IDE:

```sh
$ jdb -attach localhost:8000 -sourcepath /path/to/source
```
