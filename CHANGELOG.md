# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Standard services may be enabled using specific `ejb_app_services.toml` file.
  It supports only `configuration` and `btc-anchoring` services at the moment.

  To enable services place `ejb_app_services.toml` file in the directory 
  with EJB App with the following content: 

  ```toml
  services = ["configuration", "btc-anchoring"]
  ```

  Configuration service is enabled by default. (#338, #313)
 
- Added operations to get a message with and without signature 
  (`BinaryMessage#getMessageNoSignature` and `BinaryMessage#getSignedMessage` respectively).
  (#339)

- Methods to sign transaction messages and verify their signatures. (#341)

- Enable passing extra arguments to the JVM from the command line.
  Use `--ejb-jvm-args` flag to specify an additional argument, e.g., 
  `--ejb-jvm-args=Xmx2g`. (#342)

### Changed

- Prepended `/api` before the path to the REST API endpoints of a service. (#340)

- `Message#getSignature` returns a byte array. (#339)

## 0.1 - 2018-06-16

The first release of Exonum Java Binding.

[Unreleased]: https://github.com/exonum/exonum-java-binding/compare/v0.1...HEAD
