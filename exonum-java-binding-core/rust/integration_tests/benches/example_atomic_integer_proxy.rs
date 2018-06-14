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

extern crate java_bindings;
extern crate jni;
#[macro_use]
extern crate lazy_static;
extern crate test;

#[path = "../tests/example_proxy/mod.rs"]
mod example_proxy;
#[path = "../tests/util/mod.rs"]
mod util;

use java_bindings::DumbExecutor;
use jni::JavaVM;

use std::sync::Arc;
use test::{black_box, Bencher};

use example_proxy::AtomicIntegerProxy;
use util::create_vm;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm(false));
}

#[bench]
pub fn create_drop(b: &mut Bencher) {
    let executor = DumbExecutor { vm: Arc::clone(&VM) };
    b.iter(move || {
        black_box(AtomicIntegerProxy::new(executor.clone(), 0).unwrap())
    });
}

#[bench]
pub fn increment(b: &mut Bencher) {
    let executor = DumbExecutor { vm: Arc::clone(&VM) };
    let mut aip = AtomicIntegerProxy::new(executor, 0).unwrap();
    b.iter(move || black_box(aip.increment_and_get().unwrap()));
}

#[bench]
pub fn add(b: &mut Bencher) {
    let executor = DumbExecutor { vm: Arc::clone(&VM) };
    let mut aip = AtomicIntegerProxy::new(executor, 0).unwrap();
    b.iter(move || black_box(aip.add_and_get(2).unwrap()));
}

#[bench]
pub fn get(b: &mut Bencher) {
    let executor = DumbExecutor { vm: Arc::clone(&VM) };
    let mut aip = AtomicIntegerProxy::new(executor, 0).unwrap();
    b.iter(move || black_box(aip.get().unwrap()));
}
