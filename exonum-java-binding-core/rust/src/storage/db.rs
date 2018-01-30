use jni::JNIEnv;
use jni::objects::JClass;
use exonum::storage::{Snapshot, Fork};

use std::panic;

use utils::{self, Handle, unwrap_exc_or_default};

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub(crate) struct View {
    handle: Option<Handle>,
    internal: ViewRef,
}

pub(crate) enum ViewRef {
    Snapshot(&'static Snapshot),
    Fork(&'static mut Fork),
}

impl View {
    pub fn from_owned_snapshot(snapshot: Box<Snapshot>) -> Self {
        // Clone a reference
        // Why here is a separate reference?
        // Full `&Box<Snapshot>` type is saved into a handle to be able free it later.
        // Short `&Snapshot` type is provided by the core.
        let snapshot_ref = unsafe { &*(&*snapshot as *const Snapshot) };
        let internal = ViewRef::Snapshot(snapshot_ref);
        let handle = Some(utils::to_handle::<Box<Snapshot>>(snapshot));
        View { handle, internal }
    }

    pub fn from_owned_fork(fork: Fork) -> Self {
        // Clone a reference
        // How it works?
        // A handle is reused back as a mutable reference.
        // But it is still impossible to use more than 1 instance of an exclusive `&mut` reference
        // at once, since the handle can be used only after an instance of `View` is dropped.
        let handle = utils::to_handle::<Fork>(fork);
        let fork_ref = unsafe { &mut *(handle as *mut Fork) };
        let internal = ViewRef::Fork(fork_ref);
        View {
            handle: Some(handle),
            internal,
        }
    }

    pub fn from_ref_snapshot(snapshot: &Snapshot) -> Self {
        let snapshot_ref = unsafe { &*(&*snapshot as *const Snapshot) };
        View {
            handle: None,
            internal: ViewRef::Snapshot(snapshot_ref),
        }
    }

    pub fn from_ref_fork(fork: &mut Fork) -> Self {
        let fork_ref = unsafe { &mut *(fork as *mut Fork) };
        View {
            handle: None,
            internal: ViewRef::Fork(fork_ref),
        }
    }

    #[cfg_attr(feature = "cargo-clippy", allow(needless_borrow))]
    fn drop_if_owned(env: &JNIEnv, view_handle: Handle) {
        let res = panic::catch_unwind(|| {
            let view = utils::cast_handle::<View>(view_handle);
            if let Some(handle) = view.handle {
                match view.internal {
                    ViewRef::Snapshot(_) => utils::drop_handle::<Box<Snapshot>>(env, handle),
                    ViewRef::Fork(_) => utils::drop_handle::<Fork>(env, handle),
                };
            }
            Ok(())
        });
        unwrap_exc_or_default(env, res);
    }

    pub fn view_ref(&mut self) -> &mut ViewRef {
        &mut self.internal
    }
}

impl ViewRef {
    pub fn is_fork(&self) -> bool {
        match *self {
            ViewRef::Fork(_) => true,
            _ => false,
        }
    }

    pub fn is_snapshot(&self) -> bool {
        match *self {
            ViewRef::Snapshot(_) => true,
            _ => false,
        }
    }
}

/// Destroys underlying `Snapshot` or `Fork` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_database_Views_nativeFree(
    env: JNIEnv,
    _: JClass,
    view_handle: Handle,
) {
    View::drop_if_owned(&env, view_handle);
    utils::drop_handle::<View>(&env, view_handle);
}


#[cfg(test)]
mod tests {
    use exonum::storage::{MemoryDB, Database};
    use super::*;

    #[test]
    fn create_view() {
        let db = MemoryDB::new();
        {
            let fork = db.fork();
            let view = View::from_owned_fork(fork);
            assert!(view.handle.is_some());
            assert!(view.internal.is_fork());
        }
        {
            let snapshot = db.snapshot();
            let view = View::from_owned_snapshot(snapshot);
            assert!(view.handle.is_some());
            assert!(view.internal.is_snapshot());
        }
        {
            let mut fork = db.fork();
            let view = View::from_ref_fork(&mut fork);
            assert!(view.handle.is_none());
            assert!(view.internal.is_fork());
        }
        {
            let snapshot = db.snapshot();
            let view = View::from_ref_snapshot(&*snapshot);
            assert!(view.handle.is_none());
            assert!(view.internal.is_snapshot());
        }
    }
}
