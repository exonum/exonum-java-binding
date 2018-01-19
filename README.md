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

### How to test the native library

#### Without feature "invocation"

```$sh
$ cargo test --all
```

#### With feature "invocation"

With this feature enabled an executable binary needs `libjvm.so`/`libjvm.dylib` in order to start.
On *nix systems a path to `libjvm` should be added to environment variable LD_LIBRARY_PATH.

```$sh
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home)}"
export JAVA_LIB_PATH="$(find ${JAVA_HOME} -type f -name libjvm.\* | xargs -n1 dirname)"
export LD_LIBRARY_PATH=$JAVA_LIB_PATH:$LD_LIBRARY_PATH
```  

On OS X since El Capitan, LD_LIBRARY_PATH can't be propagated to subshells (for example, when you run a shell-script),
so LD_LIBRARY_PATH should be set in the same shell where `cargo` is run.

All integrity tests and benchmarks with `libjvm` should be placed in subcrate `dy_tests`.
Additionally, since `dy_tests` in the workspace, to exclude these tests from `--all`, feature `invocation` is used. 
Accordingly test modules have to be marked with `#![cfg(feature = "invocation")]`. 

Althou java_bindings crate organized as workspace, a bug in `cargo` prevents the use of option `--package`.
So, until it is fixed, `--manifest-path` can be used instead.
```$sh
$ cargo test --manifest-path dy_tests/Cargo.toml --features invocation
```


### Code style checks
#### Java
The style guide of the project: https://google.github.io/styleguide/javaguide.html 

**TODO:** Create a separate repo with a customized version of the code style document.
 
[Checkstyle](http://checkstyle.sourceforge.net/index.html) checks the project during `validate` phase 
(i.e., _before_ compilation). You can run code style checks explicitly via `mvn checkstyle:check`.
