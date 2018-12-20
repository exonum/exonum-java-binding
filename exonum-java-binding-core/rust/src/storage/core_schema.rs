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

use exonum::{
    blockchain::Schema,
    storage::{Fork, Snapshot, StorageValue},
};
use jni::{
    objects::JClass,
    sys::{jbyteArray, jlong, jstring},
    JNIEnv,
};
use serde_json;
use std::{panic, ptr};
use storage::db::{View, ViewRef};
use utils::{self, Handle};

type CoreSchema<T> = Schema<T>;

enum SchemaType {
    SnapshotSchema(CoreSchema<&'static Snapshot>),
    ForkSchema(CoreSchema<&'static mut Fork>),
}

/// Returns pointer to created CoreSchemaProxy object
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_blockchain_CoreSchemaProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let schema_type = match *utils::cast_handle::<View>(view_handle).get() {
            ViewRef::Snapshot(snapshot) => SchemaType::SnapshotSchema(Schema::new(snapshot)),
            ViewRef::Fork(ref mut fork) => SchemaType::ForkSchema(Schema::new(fork)),
        };
        Ok(utils::to_handle(schema_type))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `Schema` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_blockchain_CoreSchemaProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    schema_handle: Handle,
) {
    utils::drop_handle::<SchemaType>(&env, schema_handle);
}

/// Returns the height of the latest committed block. Throws `java.lang.RuntimeException` if the
/// "genesis block" has not been created yet.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_blockchain_CoreSchemaProxy_nativeGetHeight(
    env: JNIEnv,
    _: JClass,
    schema_handle: Handle,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let val: u64 = match utils::cast_handle::<SchemaType>(schema_handle) {
            SchemaType::SnapshotSchema(schema) => schema.height().into(),
            SchemaType::ForkSchema(schema) => schema.height().into(),
        };
        Ok(val as jlong)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the latest committed block. Throws `java.lang.RuntimeException` if the "genesis block"
/// has not been created yet.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_blockchain_CoreSchemaProxy_nativeGetLastBlock(
    env: JNIEnv,
    _: JClass,
    schema_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match utils::cast_handle::<SchemaType>(schema_handle) {
            SchemaType::SnapshotSchema(schema) => schema.last_block(),
            SchemaType::ForkSchema(schema) => schema.last_block(),
        };
        env.byte_array_from_slice(&val.into_bytes())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the configuration for the latest height of the blockchain. Throws
/// `java.lang.RuntimeException` if the "genesis block" has not been created yet.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_blockchain_CoreSchemaProxy_nativeGetActualConfiguration(
    env: JNIEnv,
    _: JClass,
    schema_handle: Handle,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let val = match utils::cast_handle::<SchemaType>(schema_handle) {
            SchemaType::SnapshotSchema(schema) => schema.actual_configuration(),
            SchemaType::ForkSchema(schema) => schema.actual_configuration(),
        };
        serde_json::to_string(&val)
            .map(|s| env.new_string(s))
            .unwrap()
            .map(|js| js.into_inner())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}
