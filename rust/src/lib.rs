//! Rust jni helpers for java bindings.
//!
//! All methods catch rust-panic and then rethrow java exception, but because of JNI-specific,
//! some default stub value still should be returned from the Rust side.

#![deny(missing_docs)]

#[macro_use]
extern crate log;
extern crate env_logger;
extern crate jni;
extern crate exonum;
extern crate blockchain_explorer;

mod utils;
mod init;
mod leveldb;
mod memorydb;
mod map_index;

pub use init::*;
pub use leveldb::*;
pub use memorydb::*;
pub use map_index::*;

// TODO: Use some objects (`DirectByteBuffer`?) instead of `jlong`?
// TODO: better error handling.
// TODO: Move common (non-java specific) parts into separate repository (C-bindings).
// TODO: Reduce boilerplate.
