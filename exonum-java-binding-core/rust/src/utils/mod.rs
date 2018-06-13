#![cfg_attr(feature = "cargo-clippy", deny(needless_pass_by_value))]
#![deny(non_snake_case)]

mod conversion;
mod errors;
mod exception;
mod handle;
mod jni;
mod resource_manager;
mod pair_iter;

pub use self::conversion::{convert_to_hash, convert_hash, convert_to_string};
pub use self::exception::{any_to_string, unwrap_exc_or, unwrap_exc_or_default};
pub use self::errors::{check_error_on_exception, get_and_clear_java_exception, panic_on_exception,
                       unwrap_jni, unwrap_jni_verbose};
pub use self::handle::{Handle, as_handle, cast_handle, drop_handle, to_handle};
pub use self::jni::{get_class_name, get_exception_message, get_exception_stack_trace, join_path,
                    PATH_SEPARATOR};
pub use self::pair_iter::PairIter;
pub use self::resource_manager::known_handles;
