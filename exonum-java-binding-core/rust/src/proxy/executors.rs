use jni::{JavaVM, JNIEnv};
use jni::sys::jint;

use std::mem;
use std::sync::{Arc, Mutex};

use {JniError, JniResult};
use JniErrorKind::{Other, ThreadDetached};

/// An interface for JNI thread attachment manager.
pub trait JniExecutor: Clone + Send + Sync {
    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM.
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>;
}

impl<'t, T: JniExecutor> JniExecutor for &'t T {
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        (*self).with_attached(f)
    }
}

/// A "dumb" implementation of `JniExecutor`.
/// It attaches the current thread to JVM and then detaches.
/// It just works, but it leads to very poor performance.
#[derive(Clone)]
pub struct DumbExecutor {
    /// The main JVM interface, which allows to attach threads.
    vm: &'static JavaVM,
}

impl DumbExecutor {
    /// Creates a `DumbExecutor`
    pub fn new(vm: &'static JavaVM) -> Self {
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
                if let ThreadDetached = jni_err.0 {
                    let attach_guard = self.vm.attach_current_thread()?;
                    f(&attach_guard)
                } else {
                    Err(jni_err)
                }
            }
        }
    }
}

/// A "hacky" implementation of `JniExecutor`.
///
/// It performs the JNI operations in a calling native thread.
/// This thread is attached to the JVM and is never detached (i.e., it is *leaked*).
/// Such leaks may be acceptable in applications where the number
/// of native threads _accessing the executor_ is bounded to a certain constant.
/// The executor will reject attempts to access it from a greater number of threads.
#[derive(Clone)]
pub struct HackyExecutor {
    /// The main JVM interface, which allows to attach threads.
    vm: &'static JavaVM,
    attach_limit: usize,
    num_attached_threads: Arc<Mutex<usize>>,
}

impl HackyExecutor {
    const LIMIT_EXHAUSTED: jint = 0;

    /// Creates `HackyExecutor`.
    #[cfg_attr(feature = "cargo-clippy", allow(mutex_atomic))]
    pub fn new(vm: &'static JavaVM, attach_limit: usize) -> Self {
        let num_attached_threads = Arc::new(Mutex::new(0));
        HackyExecutor {
            vm,
            attach_limit,
            num_attached_threads,
        }
    }

    fn attach_current_thread(&self) -> JniResult<JNIEnv> {
        let mut num_attached_threads = self.num_attached_threads.lock().expect(
            "Failed to acquire the mutex on the attached threads number",
        );
        if *num_attached_threads == self.attach_limit {
            Err(Other(Self::LIMIT_EXHAUSTED))?;
        }
        let attach_guard = self.vm.attach_current_thread()?;
        // We can't call detach from the right native thread,
        // so the only thing we can do is to forget to detach now.
        // JVM will detach all threads on exit.
        mem::forget(attach_guard);

        *num_attached_threads += 1;

        self.vm.get_env()
    }

    fn get_env(&self) -> JniResult<JNIEnv> {
        match self.vm.get_env() {
            Ok(jni_env) => Ok(jni_env),
            Err(jni_err) => {
                match jni_err.0 {
                    ThreadDetached => {
                        let jni_env_result = self.attach_current_thread();
                        match jni_env_result {
                            Err(JniError(ThreadDetached, ..)) => {
                                panic!("Thread should be attached");
                            }
                            Err(JniError(Other(Self::LIMIT_EXHAUSTED), ..)) => {
                                panic!(
                                    "The limit on thread attachment is exhausted (limit is {})",
                                    self.attach_limit
                                );
                            }
                            _ => jni_env_result,
                        }
                    }
                    _ => Err(jni_err),
                }
            }
        }
    }
}

impl JniExecutor for HackyExecutor {
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        f(&self.get_env()?)
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
    pub fn new(vm: &'static JavaVM) -> Self {
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
}
