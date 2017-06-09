# How to build
You need JDK 1.8+, [Maven 3](https://maven.apache.org/download.cgi) and [Rust](https://www.rust-lang.org/).
## Install system dependencies
Please install Rust and the system dependencies of 
Exonum-Core. The instructions are available [here](https://github.com/exonum/exonum-core/blob/67ac532ca2d5cf2d96ef148ae008b1599f7c9e96/INSTALL.md).
You do _not_ need to manually fetch and compile Exonum-Core.
## Build the project
To build the project, run
```$sh
$ mvn package
```
The native library will be in `rust/target/debug/`, a jar archive&mdash;in `target/`.
 
# Developer guide
## Working with Error Prone
We use [Error Prone](https://github.com/google/error-prone/) to catch common programming errors at compile time.

### How to pass a flag to Error Prone
Override `java.compiler.errorprone.flag` property:
```$sh
$ mvn -Djava.compiler.errorprone.flag=-XepAllDisabledChecksAsWarnings compile
```
Some useful flags:
 * `-XepAllErrorsAsWarnings`
 * `-XepAllDisabledChecksAsWarnings`
 * `-XepDisableAllChecks`

For a complete list of flags, go to http://errorprone.info/docs/flags 

### How to enable a particular Error Prone check
Use `java.compiler.errorprone.flag` property:
```$sh
$ mvn -Djava.compiler.errorprone.flag=-Xep:MissingOverride:ERROR compile
```
In this example, Error Prone will fail the build if any method 
does not have `@Override` annotation.

For a complete list of checks, go to http://errorprone.info/bugpatterns

### How to fix bugs found by Error Prone
Run `compile` goal in `fixerrors` profile, 
which produces a patch with suggested fixes `error-prone.patch`.

#### Produce a patch fixing any errors
```$sh
$ mvn compile -P fixerrors
```
#### Enable a particular check
Override `java.compiler.errorprone.flag` property:
```$sh
$ mvn -Djava.compiler.errorprone.flag=-Xep:MissingOverride:ERROR compile
```
In this example, Error Prone will fail the build if any method does not have `@Override` annotation.
For a complete list of checks, go to http://errorprone.info/bugpatterns
#### Produce a patch fixing particular warnings/errors
```$sh
$ mvn -Djava.compiler.errorprone.patchChecks=MissingOverride,ClassNewInstance \
        compile -P fixerrors
```
#### Enable a certain check and produce a patch
```$sh
$ mvn -Djava.compiler.errorprone.flag=-Xep:MissingOverride:ERROR \
        -Djava.compiler.errorprone.patchChecks=MissingOverride \
        compile -P fixerrors
```