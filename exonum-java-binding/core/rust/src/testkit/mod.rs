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

use jni::objects::JList;
use jni::objects::JObject;
use jni::sys::{jboolean, jshort};
use jni::JNIEnv;
use utils::Handle;

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateTestKit(
    env: JNIEnv,
    _: JObject,
    services: JList,
    auditor: jboolean,
    with_validator_count: jshort,
    _time_provider: JObject,
) -> Handle {
    Handle::default()
}

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateSnapshot(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> Handle {
    Handle::default()
}

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateBlock<'e>(
    env: JNIEnv<'e>,
    _: JObject,
    handle: Handle,
) -> JObject<'e> {
    JObject::null()
}

#[no_mangle]
#[rustfmt::skip]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateBlockWithTransactions<'e>(
    env: JNIEnv<'e>,
    _: JObject,
    handle: Handle,
    transactions: JList,
) -> JObject<'e> {
    JObject::null()
}

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeGetEmulatedNode<'e>(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> JObject<'e> {
    JObject::null()
}
