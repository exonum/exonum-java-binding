# Exonum Java Binding Contribution Guide

Exonum Java Binding is open to any contributions, whether 
it is a feedback on existing features, a request for a new one, a bug report
or a pull request. This document describes how to work with this project: 
  * how to [build](#how-to-build) it
  * how to [test](#tests) it
  * the [code style guidelines](#the-code-style)
  * how to [submit an issue](#Submitting-issues)
  * how to [submit a PR](#Submitting-pull-requests).

## How to Build
### System Dependencies
You need to install the following dependencies:
  * Linux or macOS. Windows support is coming soon. <!-- TODO: Link Java roadmap when it is published -->
  * [JDK 1.8+](http://jdk.java.net/10/).
  * [Maven 3.5+](https://maven.apache.org/download.cgi).
  * The latest stable [Rust](https://www.rust-lang.org/).
  * The [system dependencies](https://exonum.com/doc/get-started/install/) of Exonum. 
  You do _not_ need to manually fetch and compile Exonum.

### Building
Run
```$sh
$ mvn install
```

#### Building Java Binding App
Run
```$sh
$ cd exonum-java-binding-core/rust/ejb-app
$ cargo install --debug --force
```

## Modules
The project is split into several modules. Here are the main ones:
  * [`core`](exonum-java-binding-core) contains the APIs to define and implement an 
  [Exonum service](https://exonum.com/doc/get-started/design-overview/#modularity-and-services).
  * [`core-native`](exonum-java-binding-core/rust) contains the glue code between Java and Rust.
  * [`app`](exonum-java-binding-core/rust/ejb-app) is an application that runs a node with Java 
  and Rust services.
  * [`proofs`](exonum-java-proofs) provides classes to represent and verify 
  [Exonum proofs](https://exonum.com/doc/get-started/design-overview/#proofs).
  * [`exonum-service-archetype`](exonum-java-binding-service-archetype) implements an archetype
  generating a template project of Exonum Java service. 
  <!-- TODO: a link to a getting started guide/generating a project -->

## Tests
### Categories of Tests
There are several categories of tests:
  * Unit tests in Java and Rust modules.
  * Integration tests in Java, some of which require a native library.
  * Integration tests in Rust that require a JVM and `ejb-core` 
    and `ejb-fakes` artefacts. Reside 
    in [`core-native`](exonum-java-binding-core/rust/integration_tests).
  * System tests — these are currently performed internally 
    and use a [QA-service](exonum-java-binding-qa-service).

### Running Tests
<!-- TODO: Shall we explain what `mvn install` runs, and what `run_all_tests`? -->
To run the unit and integration tests, invoke this script:
```$sh
$ ./run_all_tests.sh
```

Integration tests in Rust may be run separately with this script:
```$sh
$ ./run_native_integration_tests.sh
```

### Writing Tests
#### Java
Use JUnit + [Mockito](https://github.com/mockito/mockito) or hand-written fakes.
Currently there is no project-wide default for an assertion library: 
Hamcrest and AssertJ are used in various modules.

##### Integration Tests in core
The integration tests in `core` are bound to `verify` phase of the Maven build. 
The name of IT classes must end with `IntegrationTest`. 
If your test verifies the behaviour of a class with native methods, 
or depends on such classes, the native library is required, 
which can be loaded with `LibraryLoader.load()`.

IntelliJ IDEA infers the JVM arguments from the `pom.xml` and runs ITs just fine.
If you use another IDE, configure it to pass `-Djava.library.path` system property 
to the JVM when running tests. For more details, see the failsafe plugin 
[configuration](exonum-java-binding-core/pom.xml).

#### Rust
Most Rust integration tests require a path to `libjvm.so` in `LD_LIBRARY_PATH` to run.
It’s more convenient to use the script: `./run_native_integration_tests.sh`.

## The Code Style
### Java
Java code must follow the [Google code style](https://google.github.io/styleguide/javaguide.html).
[Checkstyle](http://checkstyle.sourceforge.net/index.html) checks the project 
during `validate` phase (i.e., _before_ compilation) of the build. You can also run it manually:
```$sh
$ mvn validate
```

Development builds only report violations, pass `-P ci-build` to fail the build in case of violations.

### Rust
Rust code follows the [Rust style guide](https://github.com/rust-lang-nursery/fmt-rfcs/blob/master/guide/guide.md).
[`rustfmt`](https://github.com/rust-lang-nursery/rustfmt) enforces the code style.

After installation, you can run it with
```$sh
$ cd exonum-java-binding-core/rust
$ cargo fmt --all -- --write-mode=diff
```

## Submitting Issues
Use Github Issues to submit an issue, whether it is a question, some feedback, a bug or a feature request:
https://github.com/exonum/exonum-java-binding/issues/new

JIRA is for internal use so far and is not publicly available yet.

## Submitting Pull Requests
Before starting to work on a PR, please submit an issue describing the intended changes.
Chances are — we are already working on something similar. If not — we can then offer some
help with the requirements, design, implementation or documentation.

It’s fine to open a PR as soon as you need any feedback — ask any questions in the description.

<!-- todo: Add licensing information/CLA -->
