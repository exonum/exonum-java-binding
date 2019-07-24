# Exonum Java Benchmarks

This module contains benchmarks of various Exonum Java components.

It is configured to use [Java Microbenchmark Harness][jmh-home] (JMH).

[jmh-home]: https://openjdk.java.net/projects/code-tools/jmh/

*This module is included in the project for easier prototyping and experimentation.
It does* not *contain system-level benchmarks of Exonum services, which currently 
reside in an internal project:* 
[exonum/exonum-benchmarking](https://github.com/exonum/exonum-benchmarking)

Earlier internal micro-benchmarks are 
in [exonum/exonum-java-benchmarks](https://github.com/exonum/exonum-java-benchmarks)
project.

## Usage

Build the `benchmarks.jar` (from [exonum-java-binding](..) directory):

```
# Build the benchmarks and the modules it depends upon 
mvn package -pl benchmarks -am
```

Get some help:

```
java -jar benchmarks/target/benchmarks.jar -h
```
