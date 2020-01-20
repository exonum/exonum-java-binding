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
- Support of creation of various blockchain proofs:
    - Block Proof
    - Transaction Execution Proof
    - Call Result Proof
    - Service Data Proof.

  See [`Blockchain`][blockchain-proofs], `BlockProof` and `IndexProof`
  for details. (#1355)
- Support of creation of Protobuf-based proofs for maps and lists.
  Such proofs can be easily serialized using Protocol Buffers
  and sent to the light clients.
  See:
     - `ProofMapIndexProxy#getProof` and `MapProof`;
     - `ProofListIndexProxy.getProof`, `ProofListIndexProxy.getRangeProof` and
  `ListProof`;
     - [`Blockchain`][blockchain-proofs].
- `ProofEntryIndexProxy` collection.
- `supervisor-mode` CLI parameter added for `generate-template` command. It
  allows to configure the mode of the Supervisor service. Possible values are
  "simple" and "decentralized". (#1361)
- Service instances can be stopped and resumed now. (#1358, #1372)

[blockchain-proofs]: https://exonum.com/doc/api/java-binding/0.10.0-SNAPSHOT/com/exonum/binding/core/blockchain/Blockchain.html#proofs

### Changed
- Transactions are now implemented as service methods annotated with
  `@Transaction(TX_ID)`, instead of classes implementing
  `Transaction` _interface_. (#1274, #1307)
- Any exceptions thrown from the `Transaction` methods
  but `TransactionExecutionException` are saved with the error kind
  "unexpected" into `Blockchain#getCallErrors`.
- Redefined `TransactionExecutionException`:
  - Renamed into `ExecutionException`
  - Made `TransactionExecutionException` an unchecked (runtime) exception
  - Specified it as _the_ exception to communicate execution errors
  of `Service` methods: `@Transaction`s; `Service#afterTransactions`,
  `#initialize`; `Configurable` methods.
- Renamed `Service#beforeCommit` into `Service#afterTransactions`.
- Allowed throwing execution exceptions from `Service#afterTransactions`
  (ex. `beforeCommit`).
  Any exceptions thrown in these methods are saved in the blockchain
  in `Blockchain#getCallErrors` and can be retrieved by any services or
  light clients.
- `Blockchain#getTxResults` is replaced by `Blockchain#getCallErrors`.
  - Use `CallInBlocks` to concisely create `CallInBlock`s.
- The specification of `Configurable` operations and `Service#initialize` 
  to require throwing `ExecutionException` instead of
  `IllegalArgumentException`.
- Transaction index in block type changed from `long` to `int`. (#1348)
- Extracted artifact version to the separate field from the artifact name.
  Artifact name format is `groupId/artifactId` now.
  PluginId format is `runtimeId:artifactName:artifactVersion` now. (#1349)
- Extracted `#getIndexHash` into `HashableIndex` interface. (#1366)

### Removed
- Classes supporting no longer used tree-like list proof representation.
- `Schema#getStateHashes` and `Service#getStateHashes` methods. Framework
  automatically aggregates state hashes of the Merkelized collections.
- `TransactionConverter` — it is no longer needed with transactions
  as `Service` methods annotated with `@Transaction`. Such methods may accept
  arbitrary protobuf messages as their argument. (#1304, #1307)
- `ExecutionStatuses` factory methods (`serviceError`) as they are no longer
  useful to create _expected_ transaction execution statuses in tests —
  an `ExecutionError` now has a lot of other properties.  
  `ExecutionStatuses.success` is replaced with `ExecutionStatuses.SUCCESS` constant.

## 0.9.0-rc2 - 2019-12-17

### Fixed
- Published on Maven Central a missing dependency of a Testkit module 
(exonum-java-app).

## [0.9.0-rc1] - 2019-12-12

### Overview

The main feature of this release is support for dynamic services. Dynamic services can be added
to the blockchain network after it has been started. Since this release EJB also supports multiple
instances of the same service.
Creating proofs is not supported in this release. They will be re-enabled in one of the following
releases.

This release is based on [*Exonum 0.13.0-rc.2*][exonum-0.13].

*If you are upgrading an existing Java service, consult
the [migration guide](https://github.com/exonum/exonum-java-binding/blob/ejb/v0.9.0-rc1/exonum-java-binding/doc/Migration_guide_0.9.md).*

### Added
- Dynamic services support. (#1065, #1145, #1183)
- Exonum protobuf messages to `common` module. (#1085)
- `Service#beforeCommit` handler. (#1132)
- `TestKit` support for dynamic services. (#1145)
- Support for flat list proofs, the new compact proof format for `ProofList`. Not introduced to
  `ProofListIndexProxy` for now. (#1156)
- Java runtime plugin for exonum-launcher. (#1171)
- `serviceName` and `serviceId` were added to `TransactionContext`. They are used for creating
  schemas with unique namespaces. (#1181)
- Implement `run-dev` command support for running the node in development mode. (#1217)
- `Configurable` interface corresponding to `exonum.Configure`. (#1234)
- `ProofMapIndexProxy#truncate` and `#removeLast`. (#1272)
- Java 13 support.

### Changed
- Support for the new protobuf-based `TransactionMessage` format is provided. (#1085)
- `TimeSchema` supports multiple time service instances. (#1136)
- `TransactionResult` is replaced with `ExecutionStatus`. (#1174)
- `MapProof` enforces 32-byte long hash codes. (#1191)
- The default `ProofMapIndexProxy` implementation has been changed to hash user keys to produce an
  internal key. The implementation that does not hash the keys is still supported, see
  [documentation][proof-map-non-hashing]. (#1222)
- Updated Exonum to 0.13.0-rc.2 — see [Exonum release page][exonum-0.13]
for details.

[exonum-0.13]: https://github.com/exonum/exonum/releases/tag/v0.13.0-rc.2

### Removed
- `Service#getId` and `Service#getName` are removed. `AbstractService` now provides 
  similar methods that can be used as replacements. (#1065)
- `Blockchain#getActualConfiguration` has been replaced with
  `Blockchain#getConsensusConfiguration`, returning only the consensus configuration (now also
  containing the validator public keys) as a Protobuf message. (#1185)
- `Transaction#info` method is removed as it is no longer used by the framework. (#1225)
- `ProofMapIndexProxy#getProof` and `ProofListIndexProxy#getProof` are disabled in this release.

[proof-map-non-hashing]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/storage/indices/ProofMapIndexProxy.html#key-hashing

## [0.8.0] - 2019-09-09

### Overview

This release brings mainly internal fixes and improvements. It is based on Exonum 0.12.

### Changed
- `Ed25519CryptoFunction` to use the system libsodium by default. If libsodium is not installed,
  it will load the bundled library. (#991)
- `Ed25519CryptoFunction` is made package-private. It remains accessible via 
  `CryptoFunctions#ed25519`.
- After the introduction of MerkleDB the hash of the index is not equal to the root hash of the
  corresponding proof tree anymore. Therefore `CheckedProof#getRootHash`,
  `ProofListIndexProxy#getRootHash` and `ProofMapIndexProxy#getRootHash` are replaced with
  `CheckedProof#getIndexHash`, `ProofListIndexProxy#getIndexHash` and
  `ProofMapIndexProxy#getIndexHash` accordingly.
- Network configuration workflow. `generate-config` subcommand now accepts a single parameter -
  output directory instead of separate parameters for private and public node configs. See
  [Tutorial](./core/rust/exonum-java/TUTORIAL.md) for updated instructions.

### Added
- `stream` for sets: `KeySetIndex` and `ValueSetIndex`. (#1088)
- Proofs of absence of an element with the specified index for `ProofListIndexProxy`. (#1081)

## [0.7.0] - 2019-07-17

### Overview

This release brings support of Exonum TestKit, massive performance improvements,
and various other fixes and improvements. It is based on Exonum 0.11.

*If you are upgrading an existing Java service, consult 
the [migration guide](https://github.com/exonum/exonum-java-binding/blob/ejb/v0.7.0/exonum-java-binding/doc/Migration_guide_0.7.md).*

### Added
- A new `exonum-testkit` module that allows to emulate blockchain network and test transaction
  execution in the synchronous environment (that is, without consensus algorithm and network
  operation involved).
  Main component of this module is `TestKit` which allows recreating behavior of a single full
  node (a validator or an auditor) in an emulated Exonum blockchain network.
  For more information and examples see [documentation][testkit-documentation].
  (#819, #833, #859, #913, #989)
- Verification of native library compatibility when it is first loaded, to detect
  possible mismatch between an installed exonum-java application and the version
  used in a service project. (#882)
- `Block#isEmpty()`
- `RawTransaction#fromMessage(TransactionMessage)`, which is mostly useful in tests,
  where you might have a message but need it as a `RawTransaction` in some assertions.

[testkit-documentation]: https://exonum.com/doc/version/0.12/get-started/java-binding/#testing

### Changed
- Improved the throughput of transaction processing twofold. Java services on Exonum Java 0.7.0 
  handle transactions about 15–20% slower than equivalent Rust ones, according to our system 
  benchmarks. (#917, #996)
- `BinaryTransactionMessage#toString` to include some fields in human-readable
  format instead of the whole message in binary form.
- `Node#submitTransaction` to throw _unchecked_ `TransactionSubmissionException` instead
  of checked `InternalServerError`.
- Moved all packages inside `com.exonum.binding` to `com.exonum.binding.core` package.
  That was required to give each module a unique root package to prevent 'split-packages'
  problem. The migration guide has a regexp to update the service automatically.
- Replaced redundant `TypeAdapterFactory` in 'common' module with a single 
  `CommonTypeAdapterFactory`. `BlockTypeAdapterFactory` is renamed to `CoreTypeAdapterFactory`.
  `JsonSerializer#json` and `JsonSerializer#builder` register `CommonTypeAdapterFactory` 
  by default. `CoreTypeAdapterFactory` must be registered explicitly if needed. (#971)
- Exonum Java App now uses static linkage for RocksDB on Mac OS. Installed RocksDB
  is no more necessary to run the App. (#1011)
  
### Fixed
- The default [`Transaction#info`][tx-info-07] implementation causing an error on `transaction`
request. It is modified to return an empty object by default (no info in `content.debug` field
of the response to `transaction`). (#904)
- Allow to override root package in the template project generated with 
the exonum-java-binding-service-archetype. It remains equal to 'groupId' property
by default, but can be overridden with 'package' property.
- Application packaging issue that might have resulted in several versions of Java artifacts
on the application classpath. (#968)

[tx-info-07]: https://exonum.com/doc/api/java-binding/0.7.0/com/exonum/binding/core/transaction/Transaction.html#info()

## [0.6.0] - 2019-05-08

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
  [Contribution Guide](../CONTRIBUTING.md#building-exonum-java-app).
  (#818, #776)
- Support of multiple simultaneously active Java services on the network. To enable a list
  of specific services, you need to provide paths to each service artifact in a `services.toml` 
  configuration file. See the 
  [documentation](core/rust/exonum-java/TUTORIAL.md#services-definition) for more details. (#820)
- Internal load tests verifying the application reliability
  under various kinds of load. Builds for each release of Exonum Java, starting with 0.6.0,
  process millions of transactions and read requests to ensure stability and reliability.
- SLF4J to Log4j binding to enable libraries coded to the SLF4J API to use Log4j 2, 
  used by Exonum Java, as the implementation. (#854)
- `toOptional()` method to `EntryIndexProxy`. (#790)
- `getTransactionPool()` method to `Blockchain`. (#850)

[installation]: https://exonum.com/doc/version/0.12/get-started/java-binding/#installation

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

[Unreleased]: https://github.com/exonum/exonum-java-binding/compare/ejb/v0.9.0-rc1...HEAD
[0.9.0-rc1]: https://github.com/exonum/exonum-java-binding/compare/ejb/v0.8.0...ejb/v0.9.0-rc1
[0.8.0]: https://github.com/exonum/exonum-java-binding/compare/ejb/v0.7.0...ejb/v0.8.0
[0.7.0]: https://github.com/exonum/exonum-java-binding/compare/ejb/v0.6.0...ejb/v0.7.0
[0.6.0]: https://github.com/exonum/exonum-java-binding/compare/ejb/v0.5.0...ejb/v0.6.0
[0.5.0]: https://github.com/exonum/exonum-java-binding/compare/v0.4...ejb/v0.5.0
[0.4]: https://github.com/exonum/exonum-java-binding/compare/v0.3...v0.4
[0.3]: https://github.com/exonum/exonum-java-binding/compare/v0.2...v0.3
[0.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1.2...v0.2
[0.1.2]: https://github.com/exonum/exonum-java-binding/compare/v0.1...v0.1.2
