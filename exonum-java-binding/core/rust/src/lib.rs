// Copyright 2018 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Rust jni helpers for java bindings.
//!
//! All methods catch rust-panic and then rethrow java exception, but because of JNI-specific,
//! some default stub value still should be returned from the Rust side.

#![deny(missing_docs)]
// Function names must follow Java naming for the native functions.
#![allow(non_snake_case)]

pub extern crate exonum;
// Though we do not need exonum-btc-anchoring in libjava_bindings, it solves issue ECR-3459,
// as all RocksDB-related symbols resides inside libjava_bindings and not need to be exported for
// exonum-java.
// pub extern crate exonum_btc_anchoring;
pub extern crate exonum_rust_runtime;
pub extern crate exonum_supervisor;
pub extern crate exonum_time;
pub extern crate jni;

mod cmd;
mod handle;
mod proto;
mod proxy;
mod runtime;
mod storage;
mod testkit;
pub mod utils;

pub use self::handle::{cast_handle, drop_handle, to_handle, Handle};
pub use crate::cmd::*;
pub use crate::handle::resource_manager::*;
pub use crate::proxy::*;
pub use crate::runtime::*;
pub use crate::storage::*;
pub use crate::testkit::*;

pub use jni::errors::{Error as JniError, ErrorKind as JniErrorKind, Result as JniResult};
pub use jni::Executor;
