//! Rust jni helpers for java bindings.

#![deny(missing_docs)]

extern crate jni;
extern crate exonum;

mod utils;
mod leveldb;
mod memorydb;
mod map_index;

pub use leveldb::*;
pub use memorydb::*;
pub use map_index::*;

// TODO: Use some objects (`DirectByteBuffer`?) instead of `jlong`?
// TODO: better error handling (catch panic).
// TODO: Move common (non-java specific) parts into separate repository (C-bindings).
// TODO: Reduce boilerplate.
