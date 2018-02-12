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
extern crate exonum;
extern crate jni;
#[macro_use]
extern crate log;
extern crate serde_json;

#[cfg(feature = "resource-manager")]
#[macro_use]
extern crate lazy_static;

mod utils;
mod init;
mod proxy;
mod storage;

pub use init::*;
pub use proxy::*;
pub use storage::*;
