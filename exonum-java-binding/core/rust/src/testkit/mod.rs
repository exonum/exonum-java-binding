//private native long nativeCreateTestKit(UserServiceAdapter[] services, boolean auditor,
//short withValidatorCount, TimeProvider timeProvider);
//
//private native long nativeCreateSnapshot(long nativeHandle);
//
//private native Block nativeCreateBlock(long nativeHandle);
//
//private native Block nativeCreateBlockWithTransactions(long nativeHandle, byte[][] transactions);
//
//private native EmulatedNode nativeGetEmulatedNode(long nativeHandle);
#![allow(missing_docs)]

//com.exonum.binding.blockchain.AutoValue_Block

use exonum::blockchain::Transaction;
use exonum::messages::{BinaryForm, RawTransaction, Signed};
use exonum::storage::StorageValue;
use exonum_testkit::TestKit;
use exonum_testkit::TestKitBuilder;
use jni::objects::JList;
use jni::objects::JObject;
use jni::sys::{jboolean, jbyteArray, jshort};
use jni::JNIEnv;
use proxy::ServiceProxy;
use proxy::{MainExecutor, TransactionProxy};
use std::panic;
use std::sync::Arc;
use storage::View;
use utils::cast_handle;
use utils::unwrap_exc_or_default;
use utils::unwrap_jni;
use utils::Handle;
use utils::{to_handle, unwrap_exc_or};

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateTestKit(
    env: JNIEnv,
    _: JObject,
    services: JList,
    auditor: jboolean,
    validator_count: jshort,
    _time_provider: JObject,
) -> Handle {
    let mut builder = if auditor == jni::sys::JNI_TRUE {
        TestKitBuilder::auditor()
    } else {
        TestKitBuilder::validator()
    };
    builder = builder.with_validators(validator_count as _);
    let builder = unwrap_jni((move || {
        let executor = MainExecutor::new(Arc::new(env.get_java_vm()?));
        for service in services.iter()? {
            let global_ref = env.new_global_ref(service)?;
            let service = ServiceProxy::from_global_ref(executor.clone(), global_ref);
            builder = builder.with_service(service);
        }
        Ok(builder)
    })());
    let testkit = builder.create();
    // Mount API handlers
    testkit.api();
    let testkit = Box::new(testkit);
    let static_ref = Box::leak(testkit);
    to_handle(static_ref)
}

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateSnapshot(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<Box<TestKit>>(handle);
        let snapshot = testkit.snapshot();
        let view = View::from_owned_snapshot(snapshot);
        Ok(to_handle(view))
    });
    unwrap_exc_or_default(&env, res)
}

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateBlock(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<Box<TestKit>>(handle);
        let block = testkit.create_block().header;
        let serialized_block = block.encode().unwrap();
        let byte_array = env.byte_array_from_slice(&serialized_block)?;
        Ok(byte_array)
    });
    unwrap_exc_or(&env, res, std::ptr::null_mut())
}

#[no_mangle]
#[rustfmt::skip]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateBlockWithTransactions<'e>(
    env: JNIEnv<'e>,
    _: JObject,
    handle: Handle,
    transactions: JList,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let executor = MainExecutor::new(Arc::new(env.get_java_vm()?));
        let testkit = cast_handle::<Box<TestKit>>(handle);
        let mut raw_transactions = Vec::new();
        for object in transactions.iter()? {
            let java_byte_array: jbyteArray = object.into_inner().into();
            let byte_array = env.convert_byte_array(java_byte_array)?;
            let transaction: Signed<RawTransaction> = StorageValue::from_bytes(byte_array.into());
            raw_transactions.push(transaction);
        }
        let block = testkit.create_block_with_transactions(raw_transactions.into_iter()).header;
        let serialized_block = block.encode().unwrap();
        let byte_array = env.byte_array_from_slice(&serialized_block)?;
        Ok(byte_array)
    });
    unwrap_exc_or(&env, res, std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeGetEmulatedNode<'e>(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> JObject<'e> {
    JObject::null()
}
