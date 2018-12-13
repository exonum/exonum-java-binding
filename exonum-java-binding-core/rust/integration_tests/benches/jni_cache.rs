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

#![feature(test)]

extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate test;

use integration_tests::mock::transaction::create_mock_transaction_proxy;
use integration_tests::vm::create_vm_for_benchmarks_with_fakes;
use java_bindings::exonum::blockchain::Transaction;
use java_bindings::jni::JavaVM;
use java_bindings::MainExecutor;

use std::sync::Arc;
use test::{black_box, Bencher};

lazy_static! {
    pub static ref VM: Arc<JavaVM> = create_vm_for_benchmarks_with_fakes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

#[bench]
pub fn transaction_verify(b: &mut Bencher) {
    let tx = create_mock_transaction_proxy(EXECUTOR.clone(), true);
    b.iter(move || black_box(tx.verify()));
}
