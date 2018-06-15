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

use java_bindings::Executor;
use jni::*;
use jni::errors::Result;
use jni::objects::AutoLocal;
use jni::objects::GlobalRef;
use jni::objects::JValue;
use jni::sys::jint;

/// A temporary example of a native-to-JNI proxy
#[derive(Clone)]
pub struct AtomicIntegerProxy<E>
where
    E: Executor,
{
    exec: E,
    obj: GlobalRef,
}

impl<E> AtomicIntegerProxy<E>
where
    E: Executor,
{
    /// Creates a new instance of `AtomicIntegerProxy`
    pub fn new(exec: E, init_value: jint) -> Result<Self> {
        let obj = exec.with_attached(|env: &JNIEnv| {
            let local_ref = AutoLocal::new(
                env,
                env.new_object(
                    "java/util/concurrent/atomic/AtomicInteger",
                    "(I)V",
                    &[JValue::from(init_value)],
                )?,
            );
            env.new_global_ref(local_ref.as_obj())
        })?;
        Ok(AtomicIntegerProxy { exec, obj })
    }

    /// Gets a current value from java object
    pub fn get(&mut self) -> Result<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "get", "()I", &[])?.i()
        })
    }

    /// Increments a value of java object and then gets it
    pub fn increment_and_get(&mut self) -> Result<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "incrementAndGet", "()I", &[])?
                .i()
        })
    }

    /// Adds some value to the value of java object and then gets a resulting value
    pub fn add_and_get(&mut self, delta: jint) -> Result<jint> {
        let delta = JValue::from(delta);
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "addAndGet", "(I)I", &[delta])?
                .i()
        })
    }
}
