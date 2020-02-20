use exonum::merkledb::generic::ErasedAccess;
use jni::{
    objects::JObject,
    sys::{jbyteArray, jlong, jstring},
    JNIEnv,
};

use std::{panic, ptr};

use crate::{
    handle,
    storage::EjbAccessExt,
    utils::{self, convert_to_string, proto_to_java_bytes},
};

/// Returns IndexProof (serialized to protobuf) for specified index.
///
/// Throws exception and returns null if passed `snapshot_handle` is Fork handle.
///
/// Returns null if
/// - index is not initialized (index have not been used before calling the method)
/// - index is not Merkelized
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainProofs_nativeCreateIndexProof(
    env: JNIEnv,
    _: JObject,
    snapshot_handle: jlong,
    full_index_name: jstring,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let name = convert_to_string(&env, full_index_name)?;
        let access = handle::cast_handle::<ErasedAccess>(snapshot_handle);
        let proof = access.proof_for_index(&name);
        match proof {
            Some(proof) => proto_to_java_bytes(&env, &proof),
            None => Ok(ptr::null_mut() as jbyteArray),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns BlockProof (serialized to protobuf) for specified block.
///
/// Throws exception and returns null if
/// - there is no such block
/// - passed `snapshot_handle` is Fork handle
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_blockchain_BlockchainProofs_nativeCreateBlockProof(
    env: JNIEnv,
    _: JObject,
    snapshot_handle: jlong,
    block_height: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(snapshot_handle);
        let proof = access.proof_for_block(block_height as u64);
        match proof {
            Some(proof) => proto_to_java_bytes(&env, &proof),
            None => Ok(ptr::null_mut() as jbyteArray),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}
