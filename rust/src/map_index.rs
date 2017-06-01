use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jbyteArray};

use std::panic;

use exonum::storage2;
use utils;

// TODO: FIXME.
type MapIndex = storage2::MapIndex<(), Vec<u8>, Vec<u8>>;

/// Returns pointer to created `MapIndex` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_createNativeIndexMap(env: JNIEnv,
                                                                      _: JClass,
                                                                      _db: jlong,
                                                                      _prefix: jbyteArray)
                                                                      -> jlong {
    let res = panic::catch_unwind(|| unimplemented!());
    utils::unwrap_or_exception(env, res)
}

/// Destroys underlying `MapIndex` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_freeNativeIndexMap(env: JNIEnv,
                                                                    _: JClass,
                                                                    index: jlong) {
    utils::drop_object::<MapIndex>(env, index);
}

/// ???
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_putToIndexMap(env: JNIEnv,
                                                               _: JClass,
                                                               _key: jbyteArray,
                                                               _value: jbyteArray,
                                                               index: jlong) {
    let res = panic::catch_unwind(|| {
                                      let _index = utils::cast_object::<MapIndex>(index);
                                      unimplemented!()
                                  });
    utils::unwrap_or_exception(env, res)
}

/// ???
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_getFromIndexMap(_env: JNIEnv,
                                                                 _: JClass,
                                                                 _key: jbyteArray,
                                                                 _index: jlong)
                                                                 -> jbyteArray {
    //    let res = panic::catch_unwind(|| {
    //        let _index = utils::cast_object::<MapIndex>(index);
    //        unimplemented!()
    //    });
    //    utils::unwrap_or_exception(env, res)

    unimplemented!()
}

/// ???
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_deleteFromIndexMap(env: JNIEnv,
                                                                    _: JClass,
                                                                    _key: jbyteArray,
                                                                    index: jlong) {
    let res = panic::catch_unwind(|| {
                                      let _index = utils::cast_object::<MapIndex>(index);
                                      unimplemented!()
                                  });
    utils::unwrap_or_exception(env, res)
}
