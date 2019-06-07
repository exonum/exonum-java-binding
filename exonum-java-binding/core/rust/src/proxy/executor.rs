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

use jni::objects::JObject;
use jni::{JNIEnv, JavaVM};

use std::sync::Arc;

use JniResult;

/// The capacity of local frames, allocated for attached threads
const LOCAL_FRAME_CAPACITY: i32 = 32;

/// Jni thread attachment manager. Attaches threads as daemons, hence they do not block
/// JVM exit. Finished threads detach automatically.
#[derive(Clone)]
pub struct Executor {
    vm: Arc<JavaVM>,
}

impl Executor {
    /// Creates new Executor with specified JVM.
    pub fn new(vm: Arc<JavaVM>) -> Self {
        Self {
            vm,
        }
    }

    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM. Additionally ensures that local object references freed after call.
    /// Allocates a local frame with the specified capacity.
    pub fn with_attached_capacity<F, R>(&self, capacity: i32, f: F) -> JniResult<R>
        where
            F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        assert!(capacity > 0, "capacity should be a positive integer");

        let jni_env = self.vm.attach_current_thread_as_daemon()?;
        let mut result = None;
        jni_env.with_local_frame(capacity, || {
            result = Some(f(&jni_env));
            Ok(JObject::null())
        })?;

        result.expect("The result should be Some or this line shouldn't be reached")
    }

    /// Executes a provided closure, making sure that the current thread
    /// is attached to the JVM. Additionally ensures that local object references freed after call.
    /// Allocates a local frame with the default capacity.
    pub fn with_attached<F, R>(&self, f: F) -> JniResult<R>
        where
            F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        self.with_attached_capacity(LOCAL_FRAME_CAPACITY, f)
    }
}
