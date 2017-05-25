extern crate jni;
extern crate exonum;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jlong;

use std::panic;

use exonum::storage2::{MemoryDB, LevelDB, Database, Fork};

/// TODO: FIXME.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_MemoryDB_nativeCreateMemoryDB(_: JNIEnv, _: JClass) -> jlong {
    // TODO: Think about error handling.
    let res = panic::catch_unwind(|| Box::into_raw(Box::new(MemoryDB::new())) as jlong);
    res.unwrap_or_default()
}

/// TODO: FIXME.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_MemoryDB_nativeFreeMemoryDB(_: JNIEnv, _: JClass, db: jlong) {
    unsafe {
        Box::from_raw(db as *mut MemoryDB);
    }
}

/// TODO: FIXME.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_MemoryDB_nativeLookupSnapshot(_: JNIEnv, _: JClass, _db: jlong) -> jlong {
    unimplemented!();
    //let db = cast_object::<MemoryDB>(db);
    //Box::into_raw(db.snapshot()) as jlong
}

/// TODO: FIXME.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_MemoryDB_nativeLookupFork(_: JNIEnv, _: JClass, db: jlong) -> jlong {
    let db = cast_object::<MemoryDB>(db);
    Box::into_raw(Box::new(db.fork())) as jlong
}

/// TODO: FIXME.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_MemoryDB_nativeFreeFork(_: JNIEnv, _: JClass, fork: jlong) {
    unsafe {
        Box::from_raw(fork as *mut Fork);
    }
}

/// TODO: FIXME.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_MemoryDB_nativeCreateLevelDB(_env: JNIEnv,
                                                    _: JClass,
                                                    _path: JString)
                                                    -> jlong {
    unimplemented!();
    // TODO: `leveldb::options::Options` should be reexported.
    // TODO: Pass open options.
    //let _path = env.get_string(path).expect("Couldn't get java string!").into();
    //Box::into_raw(Box::new(LevelDB::open(path))) as jlong
}

/// TODO: FIXME.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_MemoryDB_nativeFreeLevelDB(_: JNIEnv, _: JClass, db: jlong) {
    unsafe {
        Box::from_raw(db as *mut LevelDB);
    }
}

// TODO: FIXME.
fn cast_object<T>(object: jlong) -> &'static mut T {
    let ptr = object as *mut T;
    unsafe { &mut *ptr }
}
