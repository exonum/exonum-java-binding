use jni::JNIEnv;
use jni::sys::jbyteArray;
use jni::errors::Result;

use exonum::crypto::Hash;

// Converts Java byte array to `Hash`. Panics if array has the wrong length.
pub fn convert_to_hash(env: &JNIEnv, array: jbyteArray) -> Result<Hash> {
    // TODO: Optimize copying and allocations.
    let bytes = env.convert_byte_array(array)?;
    Ok(Hash::from_slice(&bytes).expect(
        "Unable to create `Hash` from the slice",
    ))
}

// Converts `Hash` to Java byte array.
pub fn convert_hash(env: &JNIEnv, hash: &Hash) -> Result<jbyteArray> {
    env.byte_array_from_slice(hash.as_ref())
}
