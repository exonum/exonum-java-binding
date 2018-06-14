#![cfg_attr(feature = "cargo-clippy", deny(needless_pass_by_value))]
#![deny(non_snake_case)]

mod conversion;
mod exception;
mod handle;
mod pair_iter;
mod resource_manager;

pub use self::conversion::{convert_hash, convert_to_hash, convert_to_string};
pub use self::exception::{unwrap_exc_or, unwrap_exc_or_default};
pub use self::handle::{cast_handle, drop_handle, to_handle, Handle};
pub use self::pair_iter::PairIter;
pub use self::resource_manager::known_handles;
