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
- `ExonumClient#findNonEmptyBlocks` to find a certain number of the most recent non-empty
  blocks (from the last block and up to the genesis block). (#953)
- Prefix URL can be set for routing all Light Client requests. (#997) 

### Changed
- `ExonumClient#getBlocks` accepts a closed range of block heights _[from; to]_
  and returns only the blocks which heights fall into that range. The size of
  the range _(to - from + 1)_ is no longer limited. If needed, the client
  performs multiple requests to the node. (#953)
- `ExonumClient#getLastBlocks` returns only the blocks from the closed range 
  _[max(0, blockchainHeight - count + 1), blockchainHeight]_. The size of
  the range _(min(count, blockchainHeight + 1))_ is no longer limited. (#953)
- Now port is optional in the Exonum host URL. (#997) 

## 0.2.0 - 2019-05-27

Second release of Exonum Java Light Client which brings
system API and blockchain explorer API endpoints support.

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

