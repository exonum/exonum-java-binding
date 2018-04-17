use jni::{JavaVM, JNIEnv};

use std::sync::Arc;

use {JniErrorKind, JniResult};

/// An interface for JNI thread attachment manager.
pub trait JniExecutor: Clone + Send + Sync {
    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM.
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>;

    /// Returns a reference to the main JVM interface, which allows to attach threads.
    fn vm(&self) -> &Arc<JavaVM>;
}

impl<'t, T: JniExecutor> JniExecutor for &'t T {
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        (*self).with_attached(f)
    }

    fn vm(&self) -> &Arc<JavaVM> {
        (*self).vm()
    }
}

/// A "dumb" implementation of `JniExecutor`.
/// It attaches the current thread to JVM and then detaches.
/// It just works, but it leads to very poor performance.
#[derive(Clone)]
pub struct DumbExecutor {
    vm: Arc<JavaVM>,
}

impl DumbExecutor {
    /// Creates a `DumbExecutor`
    pub fn new(vm: Arc<JavaVM>) -> Self {
        DumbExecutor { vm }
    }
}

impl JniExecutor for DumbExecutor {
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        match self.vm.get_env() {
            Ok(jni_env) => f(&jni_env),
            Err(jni_err) => {
                if let JniErrorKind::ThreadDetached = jni_err.0 {
                    let attach_guard = self.vm.attach_current_thread()?;
                    f(&attach_guard)
                } else {
                    Err(jni_err)
                }
            }
        }
    }

    /// Returns a reference to the main JVM interface, which allows to attach threads.
    fn vm(&self) -> &Arc<JavaVM> {
        &self.vm
    }
}

/// An interface for JNI thread attachment manager.
/// It attaches the current thread to JVM and then detaches.
/// This struct incapsulates an actual implementation of `JniExecutor`
/// (currently - `DumbExecutor`)
#[derive(Clone)]
pub struct MainExecutor(DumbExecutor);

impl MainExecutor {
    /// Creates a `MainExecutor`
    pub fn new(vm: Arc<JavaVM>) -> Self {
        MainExecutor(DumbExecutor::new(vm))
    }
}

impl JniExecutor for MainExecutor {
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
        where
            F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        self.0.with_attached(f)
    }

    fn vm(&self) -> &Arc<JavaVM> {
        self.0.vm()
    }
}
