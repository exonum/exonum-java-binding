#![cfg(feature = "invocation")]
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
