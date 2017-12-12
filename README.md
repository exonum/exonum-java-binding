# Exonum Java Binding

[![Build Status](https://www.travis-ci.com/exonum/exonum-java-binding.svg?token=2dVYazsUZFvBqHW82g4U&branch=master)](https://www.travis-ci.com/exonum/exonum-java-binding)

## How to build
You need JDK 1.8+, [Maven 3](https://maven.apache.org/download.cgi) and [Rust](https://www.rust-lang.org/).

### Install system dependencies
Please install Rust and the system dependencies of 
Exonum. The instructions are available [here](https://github.com/exonum/exonum/blob/v0.4/INSTALL.md).
You do _not_ need to manually fetch and compile Exonum.

### Build the project
To build the project, run
```$sh
$ mvn install
```
The native library will be in `exonum-java-binding-core/rust/target/debug/`, a jar archive&mdash;in `exonum-java-binding-core/target/`.
 
## Developer guide
### Working with Error Prone
We use [Error Prone](https://github.com/google/error-prone/) to catch common programming errors at compile time.

#### How to pass a flag to Error Prone
Override `java.compiler.errorprone.flag` property:
```$sh
$ mvn -Djava.compiler.errorprone.flag=-XepAllDisabledChecksAsWarnings compile
```
Some useful flags:
 * `-XepAllErrorsAsWarnings`
 * `-XepAllDisabledChecksAsWarnings`
 * `-XepDisableAllChecks`

For a complete list of flags, go to http://errorprone.info/docs/flags 

#### How to enable a particular Error Prone check
Use `java.compiler.errorprone.flag` property:
```$sh
$ mvn -Djava.compiler.errorprone.flag=-Xep:MissingOverride:ERROR compile
```
In this example, Error Prone will fail the build if any method 
does not have `@Override` annotation.

For a complete list of checks, go to http://errorprone.info/bugpatterns

#### How to fix bugs found by Error Prone
Run `compile` goal in `fixerrors` profile, 
which produces a patch with suggested fixes `error-prone.patch`.

##### Produce a patch fixing any errors
```$sh
$ mvn compile -P fixerrors
```

##### Produce a patch fixing particular warnings/errors
```$sh
$ mvn -Djava.compiler.errorprone.patchChecks=MissingOverride,ClassNewInstance \
        compile -P fixerrors
```

##### Enable a certain check and produce a patch
```$sh
$ mvn -Djava.compiler.errorprone.flag=-Xep:MissingOverride:ERROR \
        -Djava.compiler.errorprone.patchChecks=MissingOverride \
        compile -P fixerrors
```

### Code style checks
#### Java
The style guide of the project: https://google.github.io/styleguide/javaguide.html 

**TODO:** Create a separate repo with a customized version of the code style document.
 
[Checkstyle](http://checkstyle.sourceforge.net/index.html) checks the project during `validate` phase 
(i.e., _before_ compilation). You can run code style checks explicitly via `mvn checkstyle:check`.
