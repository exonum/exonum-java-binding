use jni::{
    objects::{GlobalRef, JMethodID},
    sys::{jint, JNI_VERSION_1_8},
    JNIEnv, JavaVM,
};
use std::os::raw::c_void;

static mut OBJECT_CACHED_GET_CLASS: Option<JMethodID> = None;
static mut CLASS_CACHED_GET_NAME: Option<JMethodID> = None;
static mut THROWABLE_CACHED_GET_MESSAGE: Option<JMethodID> = None;

static mut UTA_CACHED_EXECUTE: Option<JMethodID> = None;
static mut UTA_CACHED_INFO: Option<JMethodID> = None;
static mut UTA_CACHED_VERIFY: Option<JMethodID> = None;

static mut USA_CACHED_STATE_HASHES: Option<JMethodID> = None;
static mut USA_CACHED_CONVERT_TRANSACTION: Option<JMethodID> = None;

static mut CLASS_CACHED_ERROR: Option<GlobalRef> = None;
static mut CLASS_CACHED_TEE: Option<GlobalRef> = None;

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
        OBJECT_CACHED_GET_CLASS =
            get_method_id(&env, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        CLASS_CACHED_GET_NAME =
            get_method_id(&env, "java/lang/Class", "getName", "()Ljava/lang/String;");
        THROWABLE_CACHED_GET_MESSAGE = get_method_id(
            &env,
            "java/lang/Throwable",
            "getMessage",
            "()Ljava/lang/String;",
        );
        UTA_CACHED_EXECUTE = get_method_id(
            &env,
            "com/exonum/binding/service/adapters/UserTransactionAdapter",
            "execute",
            "(J)V",
        );
        UTA_CACHED_INFO = get_method_id(
            &env,
            "com/exonum/binding/service/adapters/UserTransactionAdapter",
            "info",
            "()Ljava/lang/String;",
        );
        UTA_CACHED_VERIFY = get_method_id(
            &env,
            "com/exonum/binding/service/adapters/UserTransactionAdapter",
            "isValid",
            "()Z",
        );
        USA_CACHED_STATE_HASHES = get_method_id(
            &env,
            "com/exonum/binding/service/adapters/UserServiceAdapter",
            "getStateHashes",
            "(J)[[B",
        );
        USA_CACHED_CONVERT_TRANSACTION = get_method_id(
            &env,
            "com/exonum/binding/service/adapters/UserServiceAdapter",
            "convertTransaction",
            "([B)Lcom/exonum/binding/service/adapters/UserTransactionAdapter;",
        );
        CLASS_CACHED_ERROR = env
            .new_global_ref(env.find_class("java/lang/Error").unwrap().into())
            .ok();
        CLASS_CACHED_TEE = env
            .new_global_ref(
                env.find_class("com/exonum/binding/transaction/TransactionExecutionException")
                    .unwrap()
                    .into(),
            ).ok();

        assert!(
            OBJECT_CACHED_GET_CLASS.is_some()
                && CLASS_CACHED_ERROR.is_some()
                && THROWABLE_CACHED_GET_MESSAGE.is_some()
                && UTA_CACHED_EXECUTE.is_some()
                && UTA_CACHED_INFO.is_some()
                && UTA_CACHED_VERIFY.is_some()
                && USA_CACHED_STATE_HASHES.is_some()
                && USA_CACHED_CONVERT_TRANSACTION.is_some()
                && CLASS_CACHED_ERROR.is_some()
                && CLASS_CACHED_TEE.is_some(),
            "Error caching Java entities"
        );

        info!("Done caching references to Java classes and methods.");
    }
}

// Helper method. Produces JMethodID for a particular class dealing with lifetime.
fn get_method_id(env: &JNIEnv, class: &str, name: &str, sig: &str) -> Option<JMethodID<'static>> {
    env.get_method_id(class, name, sig)
        // we need this line to deal with lifetime
        .map(|mid| mid.into_inner().into())
        .ok()
}

/// Returns cached `JClass` for "java/lang/Error" as a GlobalRef
pub fn get_error_class() -> GlobalRef {
    unsafe { CLASS_CACHED_ERROR.clone().unwrap() }
}

/// Returns cached `JClass` for "TransactionExecutionException" as a GlobalRef
pub fn get_tee_class() -> GlobalRef {
    unsafe { CLASS_CACHED_TEE.clone().unwrap() }
}

/// Returns cached `JMethodID` for `java.lang.Object.getClass()`
pub fn get_object_get_class() -> JMethodID<'static> {
    unsafe { OBJECT_CACHED_GET_CLASS.unwrap() }
}

/// Returns cached `JMethodID` for `java.lang.Class.getName()`
pub fn get_class_get_name() -> JMethodID<'static> {
    unsafe { CLASS_CACHED_GET_NAME.unwrap() }
}

/// Returns cached `JMethodID` for `java.lang.Throwable.getMessage()`
pub fn get_throwable_get_message() -> JMethodID<'static> {
    unsafe { THROWABLE_CACHED_GET_MESSAGE.unwrap() }
}

/// Returns cached `JMethodID` for `UserTransactionAdapter.execute()`
pub fn get_uta_execute() -> JMethodID<'static> {
    unsafe { UTA_CACHED_EXECUTE.unwrap() }
}

/// Returns cached `JMethodID` for `UserTransactionAdapter.info()`
pub fn get_uta_info() -> JMethodID<'static> {
    unsafe { UTA_CACHED_INFO.unwrap() }
}

/// Returns cached `JMethodID` for `UserTransactionAdapter.isValid()`
pub fn get_uta_verify() -> JMethodID<'static> {
    unsafe { UTA_CACHED_VERIFY.unwrap() }
}

/// Returns cached `JMethodID` for `UserServiceAdapter.getStateHashes()`
pub fn get_usa_state_hashes() -> JMethodID<'static> {
    unsafe { USA_CACHED_STATE_HASHES.unwrap() }
}

/// Returns cached `JMethodID` for `UserServiceAdapter.convertTransaction()`
pub fn get_usa_convert_tx() -> JMethodID<'static> {
    unsafe { USA_CACHED_CONVERT_TRANSACTION.unwrap() }
}
