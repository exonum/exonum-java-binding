use exonum_merkledb::generic::{ErasedAccess, GenericRawAccess};
use jni::{
    objects::{JClass, JObject},
    sys::jstring,
    JNIEnv,
};

use std::panic;

use {
    handle::{self, Handle},
    utils,
};

type BlockchainData = exonum::runtime::BlockchainData<GenericRawAccess<'static>>;

/// Creates new BlockchainData for the specified `instance_name` and based on specified `base_access_handle`.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainData_nativeCreate(
    env: JNIEnv,
    _: JClass,
    base_access_handle: Handle,
    instance_name: jstring,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<GenericRawAccess<'static>>(base_access_handle);
        let instance_name = utils::convert_to_string(&env, instance_name)?;
        let blockchain_data = BlockchainData::new(access.clone(), instance_name);
        Ok(handle::to_handle(blockchain_data))
    });

    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys native BlockchainData proxy.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainData_nativeFree(
    env: JNIEnv,
    _: JClass,
    handle: Handle,
) {
    handle::drop_handle::<BlockchainData>(&env, handle);
}

/// Returns ErasedAccess for the executing service.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainData_nativeGetExecutingServiceAccess(
    env: JNIEnv,
    _: JClass,
    handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let blockchain_data = handle::cast_handle::<BlockchainData>(handle);
        let prefixed_access = blockchain_data.for_executing_service();
        let service_access = ErasedAccess::from(prefixed_access);
        Ok(handle::to_handle(service_access))
    });

    utils::unwrap_exc_or_default(&env, res)
}

/// Returns service name, which is equal to the `instance_name` the BlockchainData was created with.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainData_nativeGetServiceName(
    env: JNIEnv,
    _: JClass,
    handle: Handle,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let blockchain_data = handle::cast_handle::<BlockchainData>(handle);
        let service_name = blockchain_data.instance_name();
        let service_name = JObject::from(env.new_string(service_name)?);
        Ok(service_name.into_inner())
    });

    utils::unwrap_exc_or(&env, res, std::ptr::null_mut())
}

/// Returns read-only ErasedAccess for a specified service.
///
/// Returns null if there is no such service.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainData_nativeFindServiceData(
    env: JNIEnv,
    _: JClass,
    handle: Handle,
    service_name: jstring,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let blockchain_data = handle::cast_handle::<BlockchainData>(handle);
        let service_name = utils::convert_to_string(&env, service_name)?;
        let service_data = blockchain_data
            .for_service(service_name.as_ref())
            .map(|prefixed| ErasedAccess::from(prefixed));
        match service_data {
            Some(service_data) => Ok(handle::to_handle(service_data)),
            None => Ok(0 as Handle),
        }
    });

    utils::unwrap_exc_or_default(&env, res)
}

/// Returns mutable ErasedAccess for the whole database. Internal method, useful for implementing
/// Java proxies for Core entities.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainData_nativeGetUnstructuredAccess(
    env: JNIEnv,
    _: JClass,
    handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let blockchain_data = handle::cast_handle::<BlockchainData>(handle);
        let unstructured_access = blockchain_data.unstructured_access();
        let unstructured_access = ErasedAccess::from(unstructured_access);
        Ok(handle::to_handle(unstructured_access))
    });

    utils::unwrap_exc_or_default(&env, res)
}
