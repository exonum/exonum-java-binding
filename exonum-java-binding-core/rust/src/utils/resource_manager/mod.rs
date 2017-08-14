#[cfg(not(feature = "resource-manager"))]
#[path = "stub.rs"]
mod imp;

#[cfg(feature = "resource-manager")]
#[path = "imp.rs"]
mod imp;

pub use self::imp::*;
