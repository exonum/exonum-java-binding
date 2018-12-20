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

### Added
- Support of new transaction message format added in Exonum 0.10. (#534)
- Support of core schema API. (#548, #549, #571, #573)
- Support of `Service#afterCommit(BlockCommittedEvent event)` method
  that is invoked after each block commit event. (#550)
- Support of Json serialization in a common way. (#611)  

### Changed
- `com.exonum.binding.storage.indices.MapEntry` moved to package
  `com.exonum.binding.common.collect`. `FlatMapProof` and `MapIndex` are updated 
  to use this implementation of `MapEntry`.

### Removed
- `com.exonum.binding.common.proofs.map.MapEntry` — moved to package
  `com.exonum.binding.common.collect`.
  
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
- Standard services may be enabled using specific `ejb_app_services.toml` file.
  It supports only `configuration` and `btc-anchoring` services at the moment.

  To enable services put `ejb_app_services.toml` file into EJB App's directory
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

[Unreleased]: https://github.com/exonum/exonum-java-binding/compare/v0.3...HEAD
[0.3]: https://github.com/exonum/exonum-java-binding/compare/v0.2...v0.3
[0.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1.2...v0.2
[0.1.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1...v0.1.2
