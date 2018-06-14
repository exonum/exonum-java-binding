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

#![cfg_attr(feature = "cargo-clippy", deny(needless_pass_by_value))]
#![deny(non_snake_case)]

mod conversion;
mod exception;
mod handle;
mod resource_manager;
mod pair_iter;

pub use self::conversion::{convert_to_hash, convert_hash, convert_to_string};
pub use self::exception::{unwrap_exc_or, unwrap_exc_or_default};
pub use self::handle::{Handle, to_handle, cast_handle, drop_handle};
pub use self::pair_iter::PairIter;
pub use self::resource_manager::known_handles;
