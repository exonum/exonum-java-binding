use std::sync::Arc;

use jni::*;
use jni::JNIEnv;
use jni::errors::Result;

/// An interface for JNI thread attachment manager.
pub trait Executor: Clone {
    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM
    fn with_attached<F, R>(&self, f: F) -> Result<R>
    where
        F: FnOnce(&JNIEnv) -> Result<R>;
}

/// A "dumb" implementation of `Executor`.
/// It attaches the current thread to JVM and then detaches.
/// It just works, but it leads to very poor performance.
#[derive(Clone)]
pub struct DumbExecutor {
    /// Main JVM interface, which allows to attach threads
    pub vm: Arc<JavaVM>,
}

impl Executor for DumbExecutor {
    fn with_attached<F, R>(&self, f: F) -> Result<R>
    where
        F: FnOnce(&JNIEnv) -> Result<R>,
    {
        let env = self.vm.attach_current_thread()?;
        f(&env)
    }
}
