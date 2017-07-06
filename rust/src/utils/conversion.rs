use jni::JNIEnv;
use jni::sys::jbyteArray;

use exonum::crypto::Hash;

// Converts Java byte array to `Hash`. Panics if array has the wrong length.
pub fn convert_to_hash(env: &JNIEnv, array: jbyteArray) -> Hash {
    // TODO: Optimize copying and allocations.
    let bytes = env.convert_byte_array(array).unwrap();
    Hash::from_slice(&bytes).unwrap()
}

// Converts `Hash` to Java byte array.
pub fn convert_hash(env: &JNIEnv, hash: &Hash) -> jbyteArray {
    env.byte_array_from_slice(hash.as_ref()).unwrap()
}
