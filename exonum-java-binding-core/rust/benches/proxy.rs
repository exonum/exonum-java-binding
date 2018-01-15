#![feature(test)]

extern crate java_bindings;
extern crate jni;
#[macro_use]
extern crate lazy_static;
extern crate test;

use java_bindings::DumbExecutor;
use jni::JavaVM;
use std::sync::Arc;

use test::{black_box, Bencher};

#[path = "../tests/example_proxy/mod.rs"]
mod proxy;
use proxy::AtomicIntegerProxy;

#[path = "../src/test_util.rs"]
mod test_util;
use test_util::create_vm;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm(false));
}

#[bench]
pub fn create_drop(b: &mut Bencher) {
    let executor = DumbExecutor { vm: VM.clone() };
    b.iter(move || black_box(AtomicIntegerProxy::new(executor.clone(), 0).unwrap()));
}

#[bench]
pub fn increment(b: &mut Bencher) {
    let executor = DumbExecutor { vm: VM.clone() };
    let mut aip = AtomicIntegerProxy::new(executor, 0).unwrap();
    b.iter(move || black_box(aip.increment_and_get().unwrap()));
}

#[bench]
pub fn add(b: &mut Bencher) {
    let executor = DumbExecutor { vm: VM.clone() };
    let mut aip = AtomicIntegerProxy::new(executor, 0).unwrap();
    b.iter(move || black_box(aip.add_and_get(2).unwrap()));
}

#[bench]
pub fn get(b: &mut Bencher) {
    let executor = DumbExecutor { vm: VM.clone() };
    let mut aip = AtomicIntegerProxy::new(executor, 0).unwrap();
    b.iter(move || black_box(aip.get().unwrap()));
}
