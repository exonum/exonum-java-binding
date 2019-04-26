# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

<!-- Use the following sections from the spec: http://keepachangelog.com/en/1.0.0/
  - Added for new features.
  - Changed for changes in existing functionality.
  - Deprecated for soon-to-be removed features.
  - Removed for now removed features.
  - Fixed for any bug fixes.
  - Security in case of vulnerabilities. -->

## [Unreleased]

## [0.6.0]

**If you are upgrading an existing Java service, consult 
the [migration guide](https://github.com/exonum/exonum-java-binding/blob/ejb/v0.6.0/exonum-java-binding/doc/Migration_guide_0.6.md).**

The release is based on Exonum 0.11.

### Added
- Support of packaging the Exonum Java application into a single archive with all the necessary
  dependencies.
  This allows you to develop and run Java services without installing Rust compiler
  and building Exonum Java.
  For the instructions, consult the [Installation guide][installation].
  
  It is still possible to build the application manually, using the instructions in the
  [Contribution Guide](../CONTRIBUTING.md#Building-Exonum-Java-App).
  (#818, #776)
- Support of multiple simultaneously active Java services on the network. To enable a list
  of specific services, you need to provide paths to each service artifact in a `services.toml` 
  configuration file. See the 
  [documentation](core/rust/exonum-java/TUTORIAL.md#Services-definition) for more details. (#820)
- Internal load tests verifying the application reliability
  under various kinds of load. Builds for each release of Exonum Java, starting with 0.6.0,
  process millions of transactions and read requests to ensure stability and reliability.
- SLF4J to Log4j binding to enable libraries coded to the SLF4J API to use Log4j 2, 
  used by Exonum Java, as the implementation. (#854)
- `toOptional()` method to `EntryIndexProxy`. (#790)
- `getTransactionPool()` method to `Blockchain`. (#850)

[installation]: https://exonum.com/doc/version/latest/get-started/java-binding/#installation

### Changed
- Service HTTP APIs provided with `Service#createPublicApiHandlers` are now mounted
  on `/api/services` instead of `/api` for consistency with Exonum Core.
  
### Fixed
- The bug when Java integration tests using native library (e.g., via `MemoryDb`)
  crashed on Linux.

## [0.5.0] - 2019-03-13

### Overview

This release brings support of Exonum Time Oracle. It is based on Exonum 0.10.3.

### Added
- Support of Time oracle. Instruction on how to enable built-in services can be found
  [here](https://exonum.com/doc/version/0.10/get-started/java-binding/#built-in-services). (#667)
- [`JsonSerializer`][json-serializer-0.5.0] provides support for `ZonedDateTime`
  JSON serialization in [`ISO_ZONED_DATE_TIME`][iso-zdt-format] format. (#762)

[json-serializer-0.5.0]: https://exonum.com/doc/api/java-binding-common/0.5.0/com/exonum/binding/common/serialization/json/JsonSerializer.html
[iso-zdt-format]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#ISO_ZONED_DATE_TIME

### Changed
- `TransactionResult` and `TransactionLocation` classes moved to the common module. (#725)

## [0.4] - 2019-02-18

This release is based on the latest Exonum version, 0.10, 
and brings the features and improvements outlined below.

**If you are upgrading an existing Java service, consult 
the [migration guide](https://github.com/exonum/exonum-java-binding/blob/v0.4/exonum-java-binding/doc/Migration_guide_0.4.md).**

### Added
- Support of new transaction message format added in Exonum 0.10. It allows service 
  developers to use any serialization format, and the framework to perform signature verification
  automatically. Both the service and client APIs are migrated. (#534, #574)
- `com.exonum.binding.blockchain.Blockchain`, allowing services to read data, 
  maintained by the framework (blocks, transaction messages, transaction execution results, etc.).
  (#548, #549, #571, #573)
- Support of `Service#afterCommit(BlockCommittedEvent event)` method
  that is invoked after each block commit event. (#550)
- `com.exonum.binding.common.serialization.json.JsonSerializer` providing JSON serialization 
  support of any objects, including Exonum built-in types. (#611)
- Support of passing arbitrary arguments to the JVM when the node is launched via
  `--jvm-args-prepend` and `--jvm-args-append` CLI flags. (#629)
- `--jvm-debug` application command line argument that allows JDWP debugging of a node. (#629)
- `ListIndexProxy#stream` to enable stream processing of list elements. (#661)
- Support of running services on Java 11 runtime.

### Changed
- `com.exonum.binding.storage.indices.MapEntry` moved to package
  `com.exonum.binding.common.collect`. `FlatMapProof` and `MapIndex` are updated
  to use this implementation of `MapEntry`.
- The `--ejb-jvm-args` command line argument has been substituted with `--jvm-args-prepend` and
  `--jvm-args-append` arguments that can now be passed at the `Run` stage instead of
  `Generate-Config`. Also, these JVM arguments are not saved to any of the configuration
  files. (#629)
- `Node#getPublicKey` to return `PublicKey` instead of `byte[]`. (#651)
- `com.exonum.binding.transaction.Transaction#execute` now accepts
  `com.exonum.binding.transaction.TransactionContext`
  instead of `com.exonum.binding.storage.database.View`

### Removed
- `com.exonum.binding.common.proofs.map.MapEntry` — moved to package
  `com.exonum.binding.common.collect`.
- `ViewModificationCounter` replaced with per-`View` modification counters to simplify
  their relationship and testing. (#658)
- Exonum v0.9 message format related classes.

### Fixed
- A bug in the cryptocurrency demo frontend that sometimes resulted in rejected transactions and/or
wrong response code (#621).

## [0.3] - 2018-10-30

### Highlights

This release brings:
- Support of flat map proofs, the new compact proof format for `ProofMap`,
  supporting several keys.
- Built-in serializers of Java primitive types, some Exonum library types 
  and protobuf messages.
- Ability to report the transaction execution result as an exception with 
  extra information accessible by a client: `TransactionExecutionException`.
- A separate module `exonum-java-binding-common` that can be used in _client_
  applications to create transaction messages, check proofs, serialize/deserialize data,
  perform cryptographic operations.
- Various fixes and improvements.

The release is based on Exonum 0.8.

### Added
- Flat map proofs support, including proofs of absence and multiproofs — proofs for several
  entries at once. (#250, #507, #532)
- [`StandardSerializers`](https://exonum.com/doc/api/java-binding-common/0.3/com/exonum/binding/common/serialization/StandardSerializers.html)
  now supports `bool`, `fixed32`, `uint32`, `sint32`, `fixed64`, `uint64`, `sint64`, `float` 
  and `double` primitive types, `PrivateKey`, `PublicKey` and `byte[]` serialization. (#514, #523)
- A deterministic `Serializer` of any protobuf message — `StandardSerializers#protobuf`. (#493)
- Static factory methods accepting protobuf messages to collections,
  allowing to pass Protocol Buffer messages directly instead of using
  `StandardSerializers#protobuf`. (#505)
- `Message.Builder#setBody(byte[])` to avoid `ByteBuffer.wrap` in the client code. (#401)
- `MapIndex.isEmpty()` method to check if MapIndex is empty. (#445)
- Wallet transactions history support to the cryptocurrency-demo. (#481)

### Changed
- [`Transaction#execute`](https://exonum.com/doc/api/java-binding-core/0.3/com/exonum/binding/transaction/Transaction.html#execute-com.exonum.binding.storage.database.Fork-)
  can throw `TransactionExecutionException` to roll back 
  any changes to the database. The exception includes an error code and an optional 
  description which the framework saves to the storage for later retrieval.
  Any other exception is considered as an unexpected error (panic in Rust terms). (#392)
- Refactored `exonum-java-proofs` module to `exonum-java-binding-common` module 
  with `com.exonum.binding.common` root package so that more functionality
  is available to client applications with no dependency on `exonum-java-binding-core` (#459)
  - Moved `crypto` package to `exonum-java-binding-common` module. (#467)
  - Moved `hash` package to `exonum-java-binding-common` module. (#469)
  - Moved `Transaction`-related classes to the new `com.exonum.binding.transaction` package. (#469)
  - Moved `messages` package to `message` package in `exonum-java-binding-common` module. (#469)
  - Moved `proofs` package to `com.exonum.binding.common` package. (#469)
  - Moved `serialization` package to `com.exonum.binding.common` package. (#469)
- `ProofMapIndexProxy#getProof` to return a flat `UncheckedMapProof` 
  instead of tree-like `MapProof`, which is a more efficient format in terms of space. (#478)
- `ProofListIndexProxy#getProof` and `ProofListIndexProxy#getRangeProof` to return
  `UncheckedListProof` instead of `ListProof`. The latter is renamed into `ListProofNode`
  and may be accessed through `UncheckedListProof#getRootProofNode` (#516)
- `ListProofValidator` returns an instance of `NavigableMap` instead of `Map`. (#457)

### Removed
- `Hashing#toHexString`. (#379)
- Deprecated tree map proofs in preference to flat map proofs,
  the only supported format by the Exonum storage. (#518)

## [0.2] - 2018-07-23

### Added
- Standard services may be enabled using specific `services.toml` file.
  It supports only `configuration` and `btc-anchoring` services at the moment.

  To enable services put `services.toml` file into Exonum Java app's directory
  with the following content:
  ```toml
  services = ["configuration", "btc-anchoring"]
  ```

  Configuration service is enabled by default. (#338, #313)
- Added operations to get a message with and without signature 
  (`BinaryMessage#getMessageNoSignature` and `BinaryMessage#getSignedMessage` respectively). (#339)
- Added methods to sign transaction messages and verify their signatures. (#341)
- Enabled passing extra arguments to the JVM from the command line.
  Use `--ejb-jvm-args` flag to specify an additional argument, e.g., 
  `--ejb-jvm-args=Xmx2g`. (#342)

### Changed
- Prepended `/api` before the path to the REST API endpoints of a service. (#340)
- `Message#getSignature` returns a byte array. (#339)

### Removed
- `--ejb-debug` option — use the corresponding JVM flags, e.g.,
  `--ejb-jvm-args=Xcheck:jni`. (#342)

## [0.1.2] - 2018-07-19

Parent module and BOM module were released as they are required dependencies to define a Java service.

## 0.1 - 2018-06-16

The first release of Exonum Java Binding.

[Unreleased]: https://github.com/exonum/exonum-java-binding/compare/ejb/v0.6.0...HEAD
[0.6.0]: https://github.com/exonum/exonum-java-binding/compare/ejb/v0.5.0...ejb/v0.6.0
[0.5.0]: https://github.com/exonum/exonum-java-binding/compare/v0.4...ejb/v0.5.0
[0.4]: https://github.com/exonum/exonum-java-binding/compare/v0.3...v0.4
[0.3]: https://github.com/exonum/exonum-java-binding/compare/v0.2...v0.3
[0.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1.2...v0.2
[0.1.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1...v0.1.2
