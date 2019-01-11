/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::vm::create_vm_for_tests;
use java_bindings::jni::JavaVM;
use java_bindings::{HackyExecutor, JniExecutor};

use std::sync::Arc;
use std::thread::spawn;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = create_vm_for_tests();
}

#[test]
#[should_panic(expected = "The limit on thread attachment is exhausted (limit is 3)")]
fn exhausted_thread_limit() {
    const THREAD_NUM: usize = 4;

    let executor = HackyExecutor::new(VM.clone(), THREAD_NUM - 1);
    for _ in 0..THREAD_NUM - 1 {
        let executor = executor.clone();
        let jh = spawn(move || executor.with_attached(|_| Ok(())).unwrap());
        jh.join().unwrap();
    }
    executor.with_attached(|_| Ok(())).unwrap();
}
