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
// `JNIEnv` is passed by value to the extern functions.
#![cfg_attr(feature = "cargo-clippy", allow(needless_pass_by_value))]
// Function names must follow Java naming for the native functions.
#![allow(non_snake_case)]

pub extern crate exonum;
extern crate failure;
pub extern crate iron;
pub extern crate jni;
extern crate toml;
#[macro_use]
extern crate log;
extern crate serde;
#[macro_use]
extern crate serde_derive;
pub extern crate serde_json;

#[cfg(feature = "resource-manager")]
#[macro_use]
extern crate lazy_static;

mod error;
mod init;
mod proxy;
mod runtime;
mod storage;
#[doc(hidden)]
pub mod utils;

pub use error::*;
pub use init::*;
pub use proxy::*;
pub use runtime::*;
pub use storage::*;
