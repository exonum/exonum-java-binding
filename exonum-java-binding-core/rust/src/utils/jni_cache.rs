use jni::{
    objects::{GlobalRef, JMethodID},
    sys::{jint, JNI_VERSION_1_8},
    JNIEnv, JavaVM,
};
use std::os::raw::c_void;

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
    cache_methods(&env);

    JNI_VERSION_1_8
}

/// Caches all required classes and methods ids.
pub fn cache_methods(env: &JNIEnv) {
    unsafe {
        OBJECT_GET_CLASS =
            get_method_id(&env, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
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
            ).ok();

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
}

/// Produces `JMethodID` for a particular class dealing with its lifetime.
fn get_method_id(env: &JNIEnv, class: &str, name: &str, sig: &str) -> Option<JMethodID<'static>> {
    env.get_method_id(class, name, sig)
        // we need this line to erase lifetime in order to save underlying raw pointer in static
        .map(|mid| mid.into_inner().into())
        .ok()
}

/// Refers to the cached methods of the `UserTransactionAdapter` class.
pub mod transaction_adapter {
    use super::*;

    /// Returns cached `JMethodID` for `UserTransactionAdapter.execute()`.
    pub fn execute_id() -> JMethodID<'static> {
        unsafe { TRANSACTION_ADAPTER_EXECUTE.unwrap() }
    }

    /// Returns cached `JMethodID` for `UserTransactionAdapter.info()`.
    pub fn info_id() -> JMethodID<'static> {
        unsafe { TRANSACTION_ADAPTER_INFO.unwrap() }
    }

    /// Returns cached `JMethodID` for `UserTransactionAdapter.isValid()`.
    pub fn verify_id() -> JMethodID<'static> {
        unsafe { TRANSACTION_ADAPTER_VERIFY.unwrap() }
    }
}

/// Refers to the cached methods of the `UserServiceAdapter` class.
pub mod service_adapter {
    use super::*;

    /// Returns cached `JMethodID` for `UserServiceAdapter.getStateHashes()`.
    pub fn state_hashes_id() -> JMethodID<'static> {
        unsafe { SERVICE_ADAPTER_STATE_HASHES.unwrap() }
    }

    /// Returns cached `JMethodID` for `UserServiceAdapter.convertTransaction()`.
    pub fn convert_transaction_id() -> JMethodID<'static> {
        unsafe { SERVICE_ADAPTER_CONVERT_TRANSACTION.unwrap() }
    }
}

/// Refers to the cached methods of the `java.lang.Object` class.
pub mod object {
    use super::*;

    /// Returns cached `JMethodID` for `java.lang.Object.getClass()`.
    pub fn get_class_id() -> JMethodID<'static> {
        unsafe { OBJECT_GET_CLASS.unwrap() }
    }
}

/// Refers to the cached methods of the `java.lang.Class` class.
pub mod class {
    use super::*;

    /// Returns cached `JMethodID` for `java.lang.Class.getName()`.
    pub fn get_name_id() -> JMethodID<'static> {
        unsafe { CLASS_GET_NAME.unwrap() }
    }
}

/// Refers to the cached methods of the `java.lang.Throwable` class.
pub mod throwable {
    use super::*;

    /// Returns cached `JMethodID` for `java.lang.Throwable.getMessage()`.
    pub fn get_message_id() -> JMethodID<'static> {
        unsafe { THROWABLE_GET_MESSAGE.unwrap() }
    }
}

/// Provides access to various cached classes.
pub mod classes_refs {
    use super::*;

    /// Returns cached `JClass` for `java/lang/Error` as a `GlobalRef`.
    pub fn java_lang_error() -> GlobalRef {
        unsafe { JAVA_LANG_ERROR.clone().unwrap() }
    }

    /// Returns cached `JClass` for `TransactionExecutionException` as a `GlobalRef`.
    pub fn transaction_execution_exception() -> GlobalRef {
        unsafe { TRANSACTION_EXECUTION_EXCEPTION.clone().unwrap() }
    }
}
