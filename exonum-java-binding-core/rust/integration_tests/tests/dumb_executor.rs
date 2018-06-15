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

extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::executor::{check_detached, check_nested_attach, test_single_thread,
                                  test_serialized_threads, test_concurrent_threads};
use integration_tests::vm::create_vm_for_tests;
use java_bindings::DumbExecutor;
use java_bindings::jni::JavaVM;

use std::sync::Arc;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = create_vm_for_tests();
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor::new(VM.clone());
}

#[test]
fn single_thread() {
    test_single_thread(&*EXECUTOR);
}

#[test]
fn serialized_threads() {
    test_serialized_threads(&*EXECUTOR);
}

#[test]
fn concurrent_threads() {
    const THREAD_NUM: usize = 8;
    test_concurrent_threads(&*EXECUTOR, THREAD_NUM)
}

#[test]
fn nested_attach() {
    check_nested_attach(&VM, &*EXECUTOR);
    check_detached(&VM);
}
