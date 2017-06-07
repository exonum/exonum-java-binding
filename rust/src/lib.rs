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
mod storage;

pub use init::Java_com_exonum_binding_ClassNameTODO_nativeInitLogger;
pub use storage::{Java_com_exonum_binding_storage_DB_MemoryDB_nativeCreateMemoryDB,
                  Java_com_exonum_binding_storage_DB_MemoryDB_nativeFreeMemoryDB,
                  Java_com_exonum_binding_storage_DB_MemoryDB_nativeLookupSnapshot,
                  Java_com_exonum_binding_storage_DB_MemoryDB_nativeLookupFork,
                  Java_com_exonum_binding_storage_DB_MemoryDB_nativeFreeView,
                  Java_com_exonum_binding_storage_DB_LevelDB_nativeCreateLevelDB,
                  Java_com_exonum_binding_storage_DB_LevelDB_nativeFreeLevelDB,
                  Java_com_exonum_binding_storage_DB_LevelDB_nativeLookupSnapshot,
                  Java_com_exonum_binding_storage_DB_LevelDB_nativeLookupFork,
                  Java_com_exonum_binding_storage_DB_LevelDB_nativeFreeView,
                  Java_com_exonum_binding_index_IndexMap_createNativeIndexMap,
                  Java_com_exonum_binding_index_IndexMap_freeNativeIndexMap,
                  Java_com_exonum_binding_index_IndexMap_putToIndexMap,
                  Java_com_exonum_binding_index_IndexMap_getFromIndexMap,
                  Java_com_exonum_binding_index_IndexMap_deleteFromIndexMap};

// TODO: Use some objects (`DirectByteBuffer`?) instead of `jlong`?
// TODO: better error handling.
// TODO: Move common (non-java specific) parts into separate repository (C-bindings).
// TODO: Reduce boilerplate.
