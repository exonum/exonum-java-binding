use std::marker::{Send, Sync};
use std::sync::Arc;

use jni::*;
use jni::JNIEnv;
use jni::errors::{ErrorKind, Result};

/// An interface for JNI thread attachment manager.
pub trait Executor: Clone + Send + Sync {
    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM.
    fn with_attached<F, R>(&self, f: F) -> Result<R>
    where
        F: FnOnce(&JNIEnv) -> Result<R>;
}

/// A "dumb" implementation of `Executor`.
/// It attaches the current thread to JVM and then detaches.
/// It just works, but it leads to very poor performance.
#[derive(Clone)]
pub struct DumbExecutor {
    /// Main JVM interface, which allows to attach threads.
    pub vm: Arc<JavaVM>,
}

impl Executor for DumbExecutor {
    fn with_attached<F, R>(&self, f: F) -> Result<R>
    where
        F: FnOnce(&JNIEnv) -> Result<R>,
    {
        match self.vm.get_env() {
            Ok(jni_env) => f(&jni_env),
            Err(jni_err) => if let ErrorKind::ThreadDetached = jni_err.0 {
                let attach_guard = self.vm.attach_current_thread()?;
                f(&attach_guard)
            } else {
                Err(jni_err)
            },
        }
    }
}
