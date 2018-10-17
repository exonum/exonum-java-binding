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
- `Message.Builder#setBody(byte[])` to avoid `ByteBuffer.wrap` in the client code.
- `MapIndex.isEmpty()` method to check if MapIndex is empty.
- Flat map proofs support. (#250)
- Wallet transactions history support to the cryptocurrency-demo. (#481)
- A deterministic `Serializer` of any protobuf message — `StandardSerializers#protobuf`. (#493)
- Static factory methods accepting protobuf messages to collections,
  allowing to pass Protocol Buffer messages directly instead of using
  `StandardSerializers#protobuf`. (#505)
- `StandardSerializers` now supports `bool`, `fixed32`, `uint32`, `sint32`, 
  `fixed64`, `uint64`, `sint64`, `float` and `double` primitive types, 
  `PrivateKey`, `PublicKey` and `bytes` serialization. (#514, #523)
- Multiproofs support in `ProofMapIndexProxy`. (#507)

### Changed
- `Transaction#execute` can throw `TransactionExecutionException` to roll back 
  any changes to the database. The exception includes an error code and an optional 
  description which the framework saves to the storage for later retrieval. (#392)
- `ListProofValidator` returns an instance of `NavigableMap` instead of `Map`. (#457)
- Refactor `exonum-java-proofs` module to `exonum-java-binding-common` module 
  with `com.exonum.binding.common` root package. (#459)
  - Move `crypto` package to `exonum-java-binding-common` module. (#467)
  - Move `hash` package to `exonum-java-binding-common` module. (#469)
  - Move `Transaction`-related classes to the new `transaction` package. (#469)
  - Move `messages` package to `message` package in `exonum-java-binding-common` module. (#469)
  - Move `proofs` package to `com.exonum.binding.common` package. (#469)
  - Move `serialization` package to `com.exonum.binding.common` package. (#469)
- Replace tree proof with flat proof in `ProofMapIndexProxy`. (#478)
- Use `ByteString` instead of `byte[]` in `CheckedMapProof` interface. (#532)

### Removed
- `Hashing#toHexString`. (#379)
- Deprecated tree map proofs in preference to flat map proofs,
  the only supported format by the Exonum storage. (#518)

## 0.2 - 2018-07-23

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
  (`BinaryMessage#getMessageNoSignature` and `BinaryMessage#getSignedMessage` respectively).
  (#339)

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

## 0.1.2 - 2018-07-19

Parent module and BOM module were released as they are required dependencies to define a Java service.

## 0.1 - 2018-06-16

The first release of Exonum Java Binding.

[Unreleased]: https://github.com/exonum/exonum-java-binding/compare/v0.2...HEAD
[0.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1.2...v0.2
[0.1.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1...v0.1.2
