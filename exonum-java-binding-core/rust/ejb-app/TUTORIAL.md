This document describes how to configure and run an Exonum node with Java service using Java Binding App.

## Prerequisites

See [How to Build section of Contributing Guide][how-to-build].

You should also have ready-to-use Exonum Java service with prepared ServiceModule class.
See [Java binding documentation](https://exonum.com/doc/get-started/java-binding/).

[how-to-build]: https://github.com/exonum/exonum-java-binding/blob/master/CONTRIBUTING.md#how-to-build

## How to run an Exonum node

### Step 2. Configuring environment

#### LD_LIBRARY_PATH

`LD_LIBRARY_PATH` is used to locate native libraries used by Java Binding.
First fo all, you need to provide a path to JVM-related libraries.

Modify LD_LIBRARY_PATH environmental variable so that it contains path to `libjvm.so` file.
You can use the following script for this purpose:

```bash
JAVA_HOME="${JAVA_HOME:-$(mvn --version | grep 'Java home' | sed 's/.*: //')}"
export LD_LIBRARY_PATH="$(find ${JAVA_HOME} -type f -name libjvm.* | xargs -n1 dirname)"
```

#### CLASSPATHS

Classpath is used to locate Java classes of your service and internal ones of Java Binding.
Take classpaths for every Java library you use.

Classpath of EJB core is written into `exonum-java-binding-core/target/ejb-core-classpath.txt` file.
For your own service libary, you can use the following plugin in your `pom.xml` file:

```xml
<plugin>
    <artifactId>maven-dependency-plugin</artifactId>
    <configuration>
      <outputFile>${project.build.directory}/service-classpath.txt</outputFile>
      <includeScope>runtime</includeScope>
    </configuration>
    <executions>
      <execution>
        <id>generate-classpath-file</id>
        <goals>
          <goal>build-classpath</goal>
        </goals>
      </execution>
    </executions>
</plugin>
```

This plugin will generate classpath for your service and save it into `service-classpath.txt` file in `target` directory.

Do not forget to add `exonum-java-binding-core/target/classes` directory to your classpath collection as well.
Keep it separately, as we will use separate parameter for it while configuring Exonum node.

#### LIBPATH

Libpath is a path to native libraries used by Java Binding (for example, Java Binding needs Exonum native libraries).

Take `exonum-java-binding-core/rust/target/debug` as your `LIBPATH`.

You should also add your `LIBPATH` to your `LD_LIBRARY_PATH`.

## Step 3. Generate node configuration.

EJB App configuration is pretty similar to configuration of any other Exonum service, with a few additional parameters.

### Generate template config

```$sh
$ ejb-app generate-template testnet/common.toml
```

### Generate node private and public configs

- `--ejb-classpath` for internal classpath of Java Binding.
- `--ejb-service-classpath` for your own service classpath.
- `--ejb-libpath` for path to Java bindings native libraries.

```$sh
$ ejb-app generate-config testnet/common.toml testnet/pub.toml testnet/sec.toml \
    --ejb-classpath $EJB_CLASSPATH \
    --ejb-service-classpath $EJB_SERVICE_CLASSPATH \
    --ejb-libpath $EJB_LIBPATH \
    --peer-address 127.0.0.1:5400
```

### Finalize configuration

There are two specific parameters here:
- `--ejb-module-name` for fully qualified name of your `ServiceModule`.
- `--ejb-port` for port that your service will use for communication. Java Binding do not use Exonum Core ports directly.

```$sh
$ ejb-app finalize testnet/sec.toml testnet/node.toml \
    --ejb-module-name 'com.<company-name>.<project-name>.ServiceModule' \
    --ejb-port 6000 \
    --public-configs testnet/pub.toml
```

## Step 4. Run configured network

```$sh
$ ejb-app run -d testnet/db -c testnet/node.toml --public-api-address 127.0.0.1:3000
```