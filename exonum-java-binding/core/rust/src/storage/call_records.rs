use exonum::merkledb::generic::{ErasedAccess, GenericAccess, GenericRawAccess};
use exonum::merkledb::BinaryValue;
use jni::{
    objects::JClass,
    sys::{jbyteArray, jlong},
    JNIEnv,
};

use std::panic;

use crate::{
    handle::{self, Handle},
    utils,
};
use exonum::blockchain::{Schema, CallInBlock};
use exonum::helpers::Height;
use crate::utils::proto_to_java_bytes;

type CallRecords = exonum::blockchain::CallRecords<GenericRawAccess<'static>>;

/// Creates new BlockchainData for the specified `instance_name` and based on specified `base_access_handle`.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_CallRecords_nativeCreate(
    env: JNIEnv,
    _: JClass,
    base_access_handle: Handle,
    block_height: jlong,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess<'static>>(base_access_handle);
        let schema = match access {
            GenericAccess::Raw(raw) => Schema::new(raw.clone()),
            _ => panic!()
        };
        let call_records: CallRecords = schema.call_records(Height(block_height as u64)).unwrap();
        Ok(handle::to_handle(call_records))
    });

    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys native BlockchainData proxy.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_CallRecords_nativeFree(
    env: JNIEnv,
    _: JClass,
    handle: Handle,
) {
    handle::drop_handle::<CallRecords>(&env, handle);
}

/// Returns ErasedAccess for the executing service.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_CallRecords_nativeGet(
    env: JNIEnv,
    _: JClass,
    handle: Handle,
    call_in_block: jbyteArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let call_records = handle::cast_handle::<CallRecords>(handle);
        let call_in_block: Vec<u8> = env.convert_byte_array(call_in_block).unwrap();
        let call_in_block = CallInBlock::from_bytes(call_in_block.into()).unwrap();
        let result = call_records.get(call_in_block);
        match result {
            Ok(()) => Ok(std::ptr::null_mut() as _),
            Err(execution_error) => {
                let serialized_error = proto_to_java_bytes(&env, &execution_error).unwrap();
                Ok(serialized_error)
            },
        }
    });

    utils::unwrap_exc_or(&env, res, std::ptr::null_mut() as _)
}
