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

use java_bindings::{JniExecutor, JniResult};
use java_bindings::jni::JNIEnv;
use java_bindings::jni::objects::{GlobalRef, JValue};
use java_bindings::jni::sys::jint;

/// A test example of a native-to-JNI proxy
#[derive(Clone)]
pub struct AtomicIntegerProxy<E: JniExecutor> {
    exec: E,
    obj: GlobalRef,
}

impl<E: JniExecutor> AtomicIntegerProxy<E> {
    /// Creates a new instance of `AtomicIntegerProxy`
    pub fn new(exec: E, init_value: jint) -> JniResult<Self> {
        let obj = exec.with_attached(|env: &JNIEnv| {
            env.new_global_ref(env.new_object(
                "java/util/concurrent/atomic/AtomicInteger",
                "(I)V",
                &[JValue::from(init_value)],
            )?)
        })?;
        Ok(AtomicIntegerProxy { exec, obj })
    }

    /// Gets a current value from java object
    pub fn get(&mut self) -> JniResult<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "get", "()I", &[])?.i()
        })
    }

    /// Increments a value of java object and then gets it
    pub fn increment_and_get(&mut self) -> JniResult<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "incrementAndGet", "()I", &[])?
                .i()
        })
    }

    /// Adds some value to the value of java object and then gets a resulting value
    pub fn add_and_get(&mut self, delta: jint) -> JniResult<jint> {
        let delta = JValue::from(delta);
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "addAndGet", "(I)I", &[delta])?
                .i()
        })
    }
}
