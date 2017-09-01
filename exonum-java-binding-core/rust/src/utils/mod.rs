#![cfg_attr(feature = "cargo-clippy", deny(needless_pass_by_value))]
#![deny(non_snake_case)]

mod conversion;
mod exception;
mod handle;
mod resource_manager;
mod pair_iter;
mod local_ref;

pub use self::conversion::{convert_to_hash, convert_hash};
pub use self::exception::{unwrap_exc_or, unwrap_exc_or_default};
pub use self::handle::{Handle, to_handle, cast_handle, drop_handle};
pub use self::pair_iter::PairIter;
pub use self::local_ref::AutoLocalRef;
