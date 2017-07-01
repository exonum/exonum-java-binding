//! Rust jni helpers for java bindings.
//!
//! All methods catch rust-panic and then rethrow java exception, but because of JNI-specific,
//! some default stub value still should be returned from the Rust side.

#![deny(missing_docs)]
// `JNIEnv` is passed by value to the extern functions.
#![cfg_attr(feature = "cargo-clippy", allow(needless_pass_by_value))]
// Function names must follow Java naming for the native functions.
#![allow(non_snake_case)]

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
                  Java_com_exonum_binding_index_MapIndex_nativeCreate,
                  Java_com_exonum_binding_index_MapIndex_nativeFree,
                  Java_com_exonum_binding_index_MapIndex_nativeGet,
                  Java_com_exonum_binding_index_MapIndex_nativeContains,
                  Java_com_exonum_binding_index_MapIndex_nativePut,
                  Java_com_exonum_binding_index_MapIndex_nativeDelete,
                  Java_com_exonum_binding_index_MapIndex_nativeClear,
                  Java_com_exonum_binding_index_IndexList_nativeCreate,
                  Java_com_exonum_binding_index_IndexList_nativeFree,
                  Java_com_exonum_binding_index_IndexList_nativeGet,
                  Java_com_exonum_binding_index_IndexList_nativeLast,
                  Java_com_exonum_binding_index_IndexList_nativeIsEmpty,
                  Java_com_exonum_binding_index_IndexList_nativeLen,
                  Java_com_exonum_binding_index_IndexList_nativeIter,
                  Java_com_exonum_binding_index_IndexList_nativeIterFrom,
                  Java_com_exonum_binding_index_IndexList_nativePush,
                  Java_com_exonum_binding_index_IndexList_nativePop,
                  Java_com_exonum_binding_index_IndexList_nativeTruncate,
                  Java_com_exonum_binding_index_IndexList_nativeSet,
                  Java_com_exonum_binding_index_IndexList_nativeClear,
                  Java_com_exonum_binding_index_IndexList_nativeIterNext,
                  Java_com_exonum_binding_index_IndexList_nativeIterFree,
                  Java_com_exonum_binding_index_KeySetIndex_nativeCreate,
                  Java_com_exonum_binding_index_KeySetIndex_nativeFree,
                  Java_com_exonum_binding_index_KeySetIndex_nativeContains,
                  Java_com_exonum_binding_index_KeySetIndex_nativeIter,
                  Java_com_exonum_binding_index_KeySetIndex_nativeIterFrom,
                  Java_com_exonum_binding_index_KeySetIndex_nativeInsert,
                  Java_com_exonum_binding_index_KeySetIndex_nativeRemove,
                  Java_com_exonum_binding_index_KeySetIndex_nativeClear,
                  Java_com_exonum_binding_index_KeySetIndex_nativeIterNext,
                  Java_com_exonum_binding_index_KeySetIndex_nativeIterFree,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeCreate,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeFree,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeContains,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeContainsByHash,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeInsert,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeRemove,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeRemoveByHash,
                  Java_com_exonum_binding_index_ValueSetIndex_nativeClear};
