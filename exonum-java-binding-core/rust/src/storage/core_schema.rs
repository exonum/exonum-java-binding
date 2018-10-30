use exonum::{
    blockchain::Schema,
    storage::{Fork, Snapshot, StorageValue},
};
use jni::{
    objects::JClass,
    sys::{jbyteArray, jlong},
    JNIEnv,
};
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

/// Returns the height of the latest committed block or -1 if the "genesis block" was not created.
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
    utils::unwrap_exc_or(&env, res, -1)
}

/// Returns the latest committed block or NULL if the "genesis block" was not created.
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
