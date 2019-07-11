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

## Unreleased

### Added
- Prefix URL can be set for routing all Light Client requests. (#997) 

### Changed
- Now port is optional in the Exonum host URL. (#997) 

## 0.2.0 - 2019-05-27

Second release of Exonum Java Light Client which brings
system API and blockhain explorer API endpoints support.

### Versions Support
- Exonum version, 0.11.0
- Exonum Java Binding version, 0.6.0

### Added
- Support of [System API public][system-api-public] endpoints. (#716) 
- Support of [Explorer API][explorer-api] endpoints. (#725, #734) 

## 0.1.0 - 2019-02-18

The first release of Exonum Java Light Client.

### Versions Support
- Exonum version, 0.10
- Exonum Java Binding version, 0.4

### Highlights

This release brings:
- Support of sending transactions to Exonum blockchain nodes.
- Support of Exonum Java Binding Commons library.

[system-api-public]: https://exonum.com/doc/version/0.11/advanced/node-management/#public-endpoints
[explorer-api]: https://exonum.com/doc/version/0.11/advanced/node-management/#explorer-api-endpoints

