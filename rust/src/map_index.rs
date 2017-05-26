use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jboolean, jbyteArray};

use std::panic;

use exonum::storage2;
use utils;

// TODO: FIXME.
type MapIndex = storage2::MapIndex<(), Vec<u8>, Vec<u8>>;

/// Returns pointer to created `MapIndex` object or zero if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_createNativeIndexMap(_: JNIEnv,
                                                                      _: JClass,
                                                                      _db: jlong,
                                                                      _prefix: jbyteArray)
                                                                      -> jlong {
    unimplemented!()
}

/// Destroys underlying `MapIndex` object and frees memory. Return `false` if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_freeNativeIndexMap(_: JNIEnv,
                                                                    _: JClass,
                                                                    db: jlong)
                                                                    -> jboolean {
    utils::drop_object::<MapIndex>(db)
}

/// ???
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_putToIndexMap(_: JNIEnv,
                                                               _: JClass,
                                                               _key: jbyteArray,
                                                               _value: jbyteArray,
                                                               index: jlong)
                                                               -> jboolean {
    panic::catch_unwind(|| {
                            let _index = utils::cast_object::<MapIndex>(index);
                            unimplemented!()
                        })
            .is_ok() as jboolean
}

/// ???
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_getFromIndexMap(_: JNIEnv,
                                                                 _: JClass,
                                                                 _key: jbyteArray,
                                                                 _index: jlong)
                                                                 -> jbyteArray {
    //    panic::catch_unwind(|| {
    //        let _index = utils::cast_object::<MapIndex>(index);
    //    }).is_ok() as jboolean
    unimplemented!()
}

/// ???
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_index_IndexMap_deleteFromIndexMap(_: JNIEnv,
                                                                    _: JClass,
                                                                    _key: jbyteArray,
                                                                    index: jlong)
                                                                    -> jboolean {
    panic::catch_unwind(|| {
                            let _index = utils::cast_object::<MapIndex>(index);
                            unimplemented!()
                        })
            .is_ok() as jboolean
}
