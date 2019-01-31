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

use std::sync::Arc;

use integration_tests::executor::{
    check_attached, check_nested_attach, test_concurrent_threads, test_serialized_threads,
    test_single_thread,
};
use integration_tests::vm::create_vm_for_tests;
use java_bindings::jni::JavaVM;
use java_bindings::HackyExecutor;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = create_vm_for_tests();
}

#[test]
fn single_thread() {
    let executor = HackyExecutor::new(VM.clone(), 1);
    test_single_thread(executor);
}

#[test]
fn serialized_threads() {
    let executor = HackyExecutor::new(VM.clone(), 2);
    test_serialized_threads(executor);
}

#[test]
fn concurrent_threads() {
    const THREAD_NUM: usize = 8;
    let executor = HackyExecutor::new(VM.clone(), THREAD_NUM + 1);
    test_concurrent_threads(executor, THREAD_NUM)
}

#[test]
fn nested_attach() {
    let executor = HackyExecutor::new(VM.clone(), 1);
    check_nested_attach(&VM, executor);
    check_attached(&VM);
}
