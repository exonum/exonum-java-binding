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

use integration_tests::vm::create_vm_for_benchmarks_with_fakes;
use java_bindings::{
    jni::{
        objects::{JObject, JValue},
        JNIEnv, JavaVM,
    },
    utils::{convert_to_string, get_class_name, jni_cache},
    JniExecutor, JniResult, MainExecutor,
};

use std::sync::Arc;
use test::{black_box, Bencher};

const TX_EXEC_EXCEPTION_CLASS: &str =
    "com/exonum/binding/transaction/TransactionExecutionException";

lazy_static! {
    pub static ref VM: Arc<JavaVM> = create_vm_for_benchmarks_with_fakes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

// Returns a class name of an obj as a `String`. It's a simulation of old non cached implementation.
fn get_class_name_not_cached_impl(env: &JNIEnv, obj: JObject) -> JniResult<String> {
    let class_object = env
        .call_method(obj, "getClass", "()Ljava/lang/Class;", &[])?
        .l()?;
    let class_name = env
        .call_method(class_object, "getName", "()Ljava/lang/String;", &[])?
        .l()?;
    convert_to_string(env, class_name)
}

fn create_exception<'a>(env: &'a JNIEnv) -> JObject<'a> {
    env.new_object(TX_EXEC_EXCEPTION_CLASS, "(B)V", &[JValue::from(0 as i8)])
        .unwrap()
}

#[bench]
pub fn is_instance_of_cached(b: &mut Bencher) {
    EXECUTOR
        .with_attached(|env| {
            let exception = create_exception(env);

            b.iter(|| {
                black_box(assert!(env
                    .is_instance_of(
                        exception,
                        &jni_cache::classes_refs::transaction_execution_exception(),
                    )
                    .unwrap()))
            });
            Ok(())
        })
        .unwrap();
}

#[bench]
pub fn is_instance_of_not_cached(b: &mut Bencher) {
    EXECUTOR
        .with_attached(|env| {
            let exception = create_exception(env);
            b.iter(|| {
                black_box(assert!(env
                    .is_instance_of(exception, TX_EXEC_EXCEPTION_CLASS)
                    .unwrap()))
            });
            Ok(())
        })
        .unwrap();
}

#[bench]
pub fn get_class_name_cached(b: &mut Bencher) {
    EXECUTOR
        .with_attached(|env| {
            let obj = create_exception(env);
            b.iter(|| black_box(get_class_name(env, obj).unwrap()));
            Ok(())
        })
        .unwrap();
}

#[bench]
pub fn get_class_name_not_cached(b: &mut Bencher) {
    EXECUTOR
        .with_attached(|env| {
            let obj = create_exception(env);
            b.iter(|| black_box(get_class_name_not_cached_impl(env, obj).unwrap()));
            Ok(())
        })
        .unwrap();
}
