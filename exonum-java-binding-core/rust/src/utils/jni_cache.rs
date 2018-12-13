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

use jni::{
    objects::{GlobalRef, JMethodID},
    sys::{jint, JNI_VERSION_1_8},
    JNIEnv, JavaVM,
};
use std::{
    os::raw::c_void,
    sync::atomic::{AtomicBool, Ordering, ATOMIC_BOOL_INIT},
};

static CACHE_IS_LOCKED: AtomicBool = ATOMIC_BOOL_INIT;
static mut CACHE_INITIALIZED: bool = false;

static mut OBJECT_GET_CLASS: Option<JMethodID> = None;
static mut CLASS_GET_NAME: Option<JMethodID> = None;
static mut THROWABLE_GET_MESSAGE: Option<JMethodID> = None;

static mut TRANSACTION_ADAPTER_EXECUTE: Option<JMethodID> = None;
static mut TRANSACTION_ADAPTER_INFO: Option<JMethodID> = None;
static mut TRANSACTION_ADAPTER_VERIFY: Option<JMethodID> = None;

static mut SERVICE_ADAPTER_STATE_HASHES: Option<JMethodID> = None;
static mut SERVICE_ADAPTER_CONVERT_TRANSACTION: Option<JMethodID> = None;

static mut JAVA_LANG_ERROR: Option<GlobalRef> = None;
static mut TRANSACTION_EXECUTION_EXCEPTION: Option<GlobalRef> = None;

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _: *mut c_void) -> jint {
    let env = vm.get_env().expect("Cannot get reference to the JNIEnv");
    init_cache(&env);

    JNI_VERSION_1_8
}

/// Initializes JNI cache implementing optimal synchronization
pub fn init_cache(env: &JNIEnv) {
    while CACHE_IS_LOCKED.compare_and_swap(false, true, Ordering::Acquire) {}

    unsafe {
        if !CACHE_INITIALIZED {
            cache_methods(env);
            CACHE_INITIALIZED = true;
        }
    }

    CACHE_IS_LOCKED.store(false, Ordering::Release);
}

/// Caches all required classes and methods ids.
unsafe fn cache_methods(env: &JNIEnv) {
    OBJECT_GET_CLASS = get_method_id(&env, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    CLASS_GET_NAME = get_method_id(&env, "java/lang/Class", "getName", "()Ljava/lang/String;");
    THROWABLE_GET_MESSAGE = get_method_id(
        &env,
        "java/lang/Throwable",
        "getMessage",
        "()Ljava/lang/String;",
    );
    TRANSACTION_ADAPTER_EXECUTE = get_method_id(
        &env,
        "com/exonum/binding/service/adapters/UserTransactionAdapter",
        "execute",
        "(J)V",
    );
    TRANSACTION_ADAPTER_INFO = get_method_id(
        &env,
        "com/exonum/binding/service/adapters/UserTransactionAdapter",
        "info",
        "()Ljava/lang/String;",
    );
    TRANSACTION_ADAPTER_VERIFY = get_method_id(
        &env,
        "com/exonum/binding/service/adapters/UserTransactionAdapter",
        "isValid",
        "()Z",
    );
    SERVICE_ADAPTER_STATE_HASHES = get_method_id(
        &env,
        "com/exonum/binding/service/adapters/UserServiceAdapter",
        "getStateHashes",
        "(J)[[B",
    );
    SERVICE_ADAPTER_CONVERT_TRANSACTION = get_method_id(
        &env,
        "com/exonum/binding/service/adapters/UserServiceAdapter",
        "convertTransaction",
        "([B)Lcom/exonum/binding/service/adapters/UserTransactionAdapter;",
    );
    JAVA_LANG_ERROR = env
        .new_global_ref(env.find_class("java/lang/Error").unwrap().into())
        .ok();
    TRANSACTION_EXECUTION_EXCEPTION = env
        .new_global_ref(
            env.find_class("com/exonum/binding/transaction/TransactionExecutionException")
                .unwrap()
                .into(),
        )
        .ok();

    assert!(
        OBJECT_GET_CLASS.is_some()
            && JAVA_LANG_ERROR.is_some()
            && THROWABLE_GET_MESSAGE.is_some()
            && TRANSACTION_ADAPTER_EXECUTE.is_some()
            && TRANSACTION_ADAPTER_INFO.is_some()
            && TRANSACTION_ADAPTER_VERIFY.is_some()
            && SERVICE_ADAPTER_STATE_HASHES.is_some()
            && SERVICE_ADAPTER_CONVERT_TRANSACTION.is_some()
            && JAVA_LANG_ERROR.is_some()
            && TRANSACTION_EXECUTION_EXCEPTION.is_some(),
        "Error caching Java entities"
    );

    info!("Done caching references to Java classes and methods.");
}

/// Produces `JMethodID` for a particular class dealing with its lifetime.
fn get_method_id(env: &JNIEnv, class: &str, name: &str, sig: &str) -> Option<JMethodID<'static>> {
    env.get_method_id(class, name, sig)
        // we need this line to erase lifetime in order to save underlying raw pointer in static
        .map(|mid| mid.into_inner().into())
        .ok()
}

fn check_cache_initalized() {
    while CACHE_IS_LOCKED.compare_and_swap(false, true, Ordering::Acquire) {}
    let is_init = unsafe { CACHE_INITIALIZED };
    CACHE_IS_LOCKED.store(false, Ordering::Release);

    if !is_init {
        panic!("Cache is not initialized")
    }
}

/// Refers to the cached methods of the `UserTransactionAdapter` class.
pub mod transaction_adapter {
    use super::*;

    /// Returns cached `JMethodID` for `UserTransactionAdapter.execute()`.
    pub fn execute_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { TRANSACTION_ADAPTER_EXECUTE.unwrap() }
    }

    /// Returns cached `JMethodID` for `UserTransactionAdapter.info()`.
    pub fn info_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { TRANSACTION_ADAPTER_INFO.unwrap() }
    }

    /// Returns cached `JMethodID` for `UserTransactionAdapter.isValid()`.
    pub fn verify_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { TRANSACTION_ADAPTER_VERIFY.unwrap() }
    }
}

/// Refers to the cached methods of the `UserServiceAdapter` class.
pub mod service_adapter {
    use super::*;

    /// Returns cached `JMethodID` for `UserServiceAdapter.getStateHashes()`.
    pub fn state_hashes_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { SERVICE_ADAPTER_STATE_HASHES.unwrap() }
    }

    /// Returns cached `JMethodID` for `UserServiceAdapter.convertTransaction()`.
    pub fn convert_transaction_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { SERVICE_ADAPTER_CONVERT_TRANSACTION.unwrap() }
    }
}

/// Refers to the cached methods of the `java.lang.Object` class.
pub mod object {
    use super::*;

    /// Returns cached `JMethodID` for `java.lang.Object.getClass()`.
    pub fn get_class_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { OBJECT_GET_CLASS.unwrap() }
    }
}

/// Refers to the cached methods of the `java.lang.Class` class.
pub mod class {
    use super::*;

    /// Returns cached `JMethodID` for `java.lang.Class.getName()`.
    pub fn get_name_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { CLASS_GET_NAME.unwrap() }
    }
}

/// Refers to the cached methods of the `java.lang.Throwable` class.
pub mod throwable {
    use super::*;

    /// Returns cached `JMethodID` for `java.lang.Throwable.getMessage()`.
    pub fn get_message_id() -> JMethodID<'static> {
        check_cache_initalized();
        unsafe { THROWABLE_GET_MESSAGE.unwrap() }
    }
}

/// Provides access to various cached classes.
pub mod classes_refs {
    use super::*;

    /// Returns cached `JClass` for `java/lang/Error` as a `GlobalRef`.
    pub fn java_lang_error() -> GlobalRef {
        check_cache_initalized();
        unsafe { JAVA_LANG_ERROR.clone().unwrap() }
    }

    /// Returns cached `JClass` for `TransactionExecutionException` as a `GlobalRef`.
    pub fn transaction_execution_exception() -> GlobalRef {
        check_cache_initalized();
        unsafe { TRANSACTION_EXECUTION_EXCEPTION.clone().unwrap() }
    }
}
