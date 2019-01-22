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

use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::utils::jni_cache;

use std::{
    sync::{Arc, Barrier},
    thread::spawn,
};


#[test]
// NOTE: This test is not supposed to reliably catch synchronization errors.
fn concurrent_cache_read() {
    const THREAD_NUM: usize = 8;
    let mut threads = Vec::new();

    create_vm_for_tests_with_fake_classes();

    let barrier = Arc::new(Barrier::new(THREAD_NUM));

    for _ in 0..THREAD_NUM {
        let barrier = Arc::clone(&barrier);
        let jh = spawn(move || {
            barrier.wait();
            jni_cache::transaction_adapter::execute_id();
            jni_cache::transaction_adapter::info_id();
            jni_cache::service_adapter::convert_transaction_id();
            jni_cache::service_adapter::state_hashes_id();
            jni_cache::class::get_name_id();
            jni_cache::object::get_class_id();
            jni_cache::classes_refs::java_lang_error();
            jni_cache::classes_refs::transaction_execution_exception();
        });
        threads.push(jh);
    }
    for jh in threads {
        jh.join().unwrap();
    }
}

#[test]
#[should_panic(expected = "Cache is not initialized")]
fn cache_not_initialized() {
    jni_cache::transaction_adapter::execute_id();
}
