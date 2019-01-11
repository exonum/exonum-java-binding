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

use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::{
    jni::{objects::JThrowable, JNIEnv, JavaVM},
    utils::{
        check_error_on_exception, get_and_clear_java_exception, get_class_name,
        get_exception_message, jni_cache, panic_on_exception,
    },
    JniErrorKind, JniExecutor, JniResult, MainExecutor,
};
use std::sync::Arc;

const ERROR_CLASS: &str = "java/lang/Error";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";
const EXCEPTION_CLASS: &str = "java/lang/Exception";
const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const ARITHMETIC_EXCEPTION_CLASS_FQN: &str = "java.lang.ArithmeticException";
const CUSTOM_EXCEPTION_MESSAGE: &str = "Test exception message";

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = {
        let ex = MainExecutor::new(VM.clone());
        // JNI_OnLoad() is not called for these tests
        ex.with_attached(|env|{
            jni_cache::init_cache(env);
            Ok(())
        }).unwrap();
        ex
    };
}

#[test]
#[should_panic(expected = "Java exception: java.lang.Exception")]
fn panic_on_exception_catch_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, throw(env, EXCEPTION_CLASS));
            Ok(())
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
fn panic_on_exception_catch_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS));
            Ok(())
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "JNI error: ")]
fn panic_on_exception_catch_jni_error() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, make_jni_error());
            Ok(())
        })
        .unwrap();
}

#[test]
fn panic_on_exception_dont_catch_good_result() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, Ok(()));
            Ok(())
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.Error")]
fn check_error_on_exception_catch_java_error_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_error_on_exception(env, throw(env, ERROR_CLASS))))
        .unwrap()
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn check_error_on_exception_catch_java_error_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_error_on_exception(env, throw(env, OOM_ERROR_CLASS)))
        })
        .unwrap()
        .unwrap();
}

#[test]
fn check_error_on_exception_catch_java_exception_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            check_error_on_exception(env, throw(env, EXCEPTION_CLASS))
                .map_err(|e| assert!(e.starts_with("Java exception: java.lang.Exception")))
                .expect_err("An exception should lead to an error");
            Ok(())
        })
        .unwrap();
}

#[test]
fn check_error_on_exception_catch_java_exception_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            check_error_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS))
                .map_err(
                    |e| assert!(e.starts_with("Java exception: java.lang.ArithmeticException")),
                )
                .expect_err("An exception should lead to an error");
            Ok(())
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "JNI error: ")]
fn check_error_on_exception_catch_jni_error() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_error_on_exception(env, make_jni_error())))
        .unwrap()
        .unwrap();
}

#[test]
fn check_error_on_exception_dont_catch_good_result() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_error_on_exception(env, Ok(()))))
        .unwrap()
        .unwrap();
}

#[test]
fn get_and_clear_java_exception_if_exception_occurred() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            // Error should be handled, not raised
            throw(env, ARITHMETIC_EXCEPTION_CLASS).unwrap_err();
            let exception = get_and_clear_java_exception(env);
            assert_eq!(
                get_class_name(env, exception)?,
                ARITHMETIC_EXCEPTION_CLASS_FQN
            );
            assert!(!env.exception_check()?);
            Ok(())
        })
        .unwrap();
}

#[test]
fn get_exception_message_without_message() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            let exception = env.new_object(ARITHMETIC_EXCEPTION_CLASS, "()V", &[])?;
            assert_eq!(get_exception_message(env, exception)?, None);
            Ok(())
        })
        .unwrap();
}

#[test]
fn get_exception_message_with_message() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            throw_with_message(env, ARITHMETIC_EXCEPTION_CLASS, CUSTOM_EXCEPTION_MESSAGE)
                .unwrap_err();
            let exception = get_and_clear_java_exception(env);
            assert_eq!(
                get_exception_message(env, exception)?,
                Some(CUSTOM_EXCEPTION_MESSAGE.to_string())
            );
            Ok(())
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "No exception thrown")]
fn get_and_clear_java_exception_if_no_exception_occurred() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            let _exception = get_and_clear_java_exception(env);
            Ok(())
        })
        .unwrap();
}

fn throw(env: &JNIEnv, exception_class: &str) -> JniResult<()> {
    let ex: JThrowable = env.new_object(exception_class, "()V", &[])?.into();
    env.throw(ex)?;
    Err(JniErrorKind::JavaException.into())
}

fn throw_with_message(env: &JNIEnv, exception_class: &str, message: &str) -> JniResult<()> {
    env.throw((exception_class, message))?;
    Err(JniErrorKind::JavaException.into())
}

fn make_jni_error() -> JniResult<()> {
    Err(JniErrorKind::Msg("Custom test error".to_string()).into())
}
