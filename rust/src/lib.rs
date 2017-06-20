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

mod utils;
mod init;
mod storage;

pub use init::Java_com_exonum_binding_ClassNameTODO_nativeInitLogger;
pub use storage::{Java_com_exonum_binding_storage_connector_Views_nativeFree,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeCreate,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeFree,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeLookupSnapshot,
                  Java_com_exonum_binding_storage_db_MemoryDb_nativeLookupFork,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeCreate,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeFree,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeLookupSnapshot,
                  Java_com_exonum_binding_storage_db_LevelDb_nativeLookupFork,
                  Java_com_exonum_binding_index_IndexMap_nativeCreate,
                  Java_com_exonum_binding_index_IndexMap_nativeFree,
                  Java_com_exonum_binding_index_IndexMap_nativeGet,
                  Java_com_exonum_binding_index_IndexMap_nativeContains,
                  Java_com_exonum_binding_index_IndexMap_nativePut,
                  Java_com_exonum_binding_index_IndexMap_nativeDelete,
                  Java_com_exonum_binding_index_IndexMap_nativeClear};
