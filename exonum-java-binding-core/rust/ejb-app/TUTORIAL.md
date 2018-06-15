This document describes how to configure and run an Exonum node with Java service using Java Binding App.

## Prerequisites

See [How to Build section of Contribution Guide][how-to-build].

You should also have ready-to-use Exonum Java service with prepared ServiceModule class.
See [Java binding documentation](https://exonum.com/doc/get-started/java-binding/).

[how-to-build]: https://github.com/exonum/exonum-java-binding/blob/master/CONTRIBUTING.md#how-to-build

## How to Run an Exonum Node

### Step 1. Configure Environment

`EJB_ROOT` used in examples of this section corresponds to the Java Binding root directory.

#### LD_LIBRARY_PATH

`LD_LIBRARY_PATH` is required to locate native libraries used by Java Binding.
You need to provide a path to the JVM library (`libjvm.so`) and to the Rust standard library.

You can use the following script for this purpose:

```bash
JAVA_HOME="${JAVA_HOME:-$(mvn --version | grep 'Java home' | sed 's/.*: //')}"
LIBJVM_DIR="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
RUST_LIB_DIR="$(rustup run stable rustc --print sysroot)/lib"

export LD_LIBRARY_PATH="$RUST_LIB_DIR:$LIBJVM_DIR"
```

#### CLASSPATH
Classpath is used to locate Java classes of your service and its dependencies, including 
the Exonum Java Binding.

You may package your service in an Uber JAR using 
the [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/index.html)
and pass a path to the service artefact during application configuration as `--ejb-classpath`
parameter. Alternatively, you may assemble a classpath that includes the service and all of 
its dependencies and pass it instead.

#### LIBPATH

Libpath is a path to native libraries used by Java Binding (for example, Java Binding needs Exonum native libraries).

Take `$EJB_ROOT/exonum-java-binding-core/rust/target/debug` as your `LIBPATH`.

You should also add your `LIBPATH` to your `LD_LIBRARY_PATH`.


### Step 2. Generate Node Configuration

EJB App configuration is pretty similar to configuration of any other Exonum service, with a few additional parameters.

#### Generate Template Config

```$sh
$ ejb-app generate-template testnet/common.toml
```

#### Generate Node Private and Public Configs

- `--ejb-classpath` for a classpath of your service.
- `--ejb-libpath` for a path to Java bindings native libraries.

```$sh
$ ejb-app generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
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
$ ejb-app finalize testnet/sec.toml testnet/node.toml \
    --ejb-module-name 'com.<company-name>.<project-name>.ServiceModule' \
    --ejb-port 6000 \
    --public-configs testnet/pub.toml
```

### Step 3. Run Configured Node

```$sh
$ ejb-app run -d testnet/db -c testnet/node.toml --public-api-address 127.0.0.1:3000
```
