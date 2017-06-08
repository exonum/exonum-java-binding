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
 