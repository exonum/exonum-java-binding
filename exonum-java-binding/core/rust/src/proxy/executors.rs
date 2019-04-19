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

use jni::{
    JNIEnv, JavaVM,
    {objects::JObject, sys::jint},
};

use std::{
    cmp, mem,
    sync::{Arc, Mutex},
    thread,
};

use JniErrorKind::{Other, ThreadDetached};
use {JniError, JniResult};

/// An interface for JNI thread attachment manager.
pub trait JniExecutor: Clone + Send + Sync {
    /// The capacity of local frames, allocated for attached threads
    const LOCAL_FRAME_CAPACITY: i32 = 32;

    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM. Additionally ensures that local object references freed after call.
    /// Allocates a local frame with the specified capacity.
    fn with_attached_capacity<F, R>(&self, capacity: i32, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        assert!(capacity > 0, "capacity should be a positive integer");
        self.with_attached_impl(|jni_env| {
            let mut result = None;
            jni_env.with_local_frame(capacity, || {
                result = Some(f(jni_env));
                Ok(JObject::null())
            })?;
            result.expect("The result should be Some or this line shouldn't be reached")
        })
    }

    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM. Additionally ensures that local object references freed after call.
    /// Allocates a local frame with the default capacity.
    fn with_attached<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        self.with_attached_capacity(Self::LOCAL_FRAME_CAPACITY, f)
    }

    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM. Does not allocate local frame.
    /// Should not be used while it is possible to use #with_attached.
    #[doc(hidden)]
    fn with_attached_impl<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>;
}

impl<'t, T: JniExecutor> JniExecutor for &'t T {
    fn with_attached_impl<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        (*self).with_attached_impl(f)
    }
}

/// A "dumb" implementation of `JniExecutor`.
/// It attaches the current thread to JVM and then detaches.
/// It just works, but it leads to very poor performance.
#[derive(Clone)]
pub struct DumbExecutor {
    /// The main JVM interface, which allows to attach threads.
    vm: Arc<JavaVM>,
}

impl DumbExecutor {
    /// Creates a `DumbExecutor`
    pub fn new(vm: Arc<JavaVM>) -> Self {
        DumbExecutor { vm }
    }
}

impl JniExecutor for DumbExecutor {
    fn with_attached_impl<F, R>(&self, f: F) -> JniResult<R>
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
    vm: Arc<JavaVM>,
    attach_limit: usize,
    num_attached_threads: Arc<Mutex<usize>>,
}

impl HackyExecutor {
    const LIMIT_EXHAUSTED: jint = 0;

    /// Creates `HackyExecutor`.
    #[allow(clippy::mutex_atomic)]
    pub fn new(vm: Arc<JavaVM>, attach_limit: usize) -> Self {
        let num_attached_threads = Arc::new(Mutex::new(0));
        HackyExecutor {
            vm,
            attach_limit,
            num_attached_threads,
        }
    }

    fn attach_current_thread(&self) -> JniResult<JNIEnv> {
        let mut num_attached_threads = self
            .num_attached_threads
            .lock()
            .expect("Failed to acquire the mutex on the attached threads number");

        // Dump info about thread to attach and num of threads attached.
        let curr_thread = thread::current();
        let curr_thread_name = curr_thread.name().unwrap_or("n/a");
        info!(
            "Attaching thread with id: {:?} and name: {}. Number of threads already attached: {}.",
            curr_thread.id(),
            curr_thread_name,
            *num_attached_threads
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
            Err(jni_err) => match jni_err.0 {
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
            },
        }
    }
}

impl JniExecutor for HackyExecutor {
    fn with_attached_impl<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        f(&self.get_env()?)
    }
}

/// An interface for JNI thread attachment manager.
/// It attaches the current thread to JVM and then detaches.
/// This struct encapsulates an actual implementation of `JniExecutor`
/// (currently - `HackyExecutor`)
#[derive(Clone)]
pub struct MainExecutor(HackyExecutor);

impl MainExecutor {
    /// Creates a `MainExecutor`
    pub fn new(vm: Arc<JavaVM>) -> Self {
        let threads_limit = cmp::max(16, num_cpus::get() * 2);
        MainExecutor(HackyExecutor::new(vm, threads_limit))
    }
}

impl JniExecutor for MainExecutor {
    fn with_attached_impl<F, R>(&self, f: F) -> JniResult<R>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        self.0.with_attached_impl(f)
    }
}
