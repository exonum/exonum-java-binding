#![feature(test)]

extern crate jni;
extern crate java_bindings;
extern crate test;

#[macro_use]
extern crate lazy_static;

use jni::{InitArgsBuilder, JNIVersion, JavaVM};
use std::sync::Arc;
use test::{Bencher, black_box};

use java_bindings::AtomicIntegerProxy;
use java_bindings::DumbExecutor;


lazy_static! {
    static ref VM: Arc<JavaVM> = {
        let jvm_args = InitArgsBuilder::new()
            .version(JNIVersion::V8)
            .build()
            .unwrap_or_else(|e| panic!(format!("{:#?}", e)));

        let jvm = JavaVM::new(jvm_args)
            .unwrap_or_else(|e| panic!(format!("{:#?}", e)));

        Arc::new(jvm)
    };
}


#[bench]
pub fn create_drop_dumb(b: &mut Bencher) {
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
