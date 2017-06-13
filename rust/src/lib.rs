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
pub use storage::{Java_com_exonum_binding_storage_db_MemoryDb_nativeCreateMemoryDb,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeFreeMemoryDb,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeLookupSnapshot,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeLookupFork,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeFreeView,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeCreateLevelDb,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeFreeLevelDb,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeLookupSnapshot,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeLookupFork,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeFreeView,
                  Java_com_exonum_binding_index_IndexMap_createNativeIndexMap,
                  Java_com_exonum_binding_index_IndexMap_freeNativeIndexMap,
                  Java_com_exonum_binding_index_IndexMap_putToIndexMap,
                  Java_com_exonum_binding_index_IndexMap_getFromIndexMap,
                  Java_com_exonum_binding_index_IndexMap_deleteFromIndexMap};
