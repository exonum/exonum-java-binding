use exonum::crypto::Hash;
use jni::JNIEnv;
use jni::sys::jbyteArray;
use jni::objects::JString;

use JniResult;

// Converts Java byte array to `Hash`. Panics if array has the wrong length.
pub fn convert_to_hash(env: &JNIEnv, array: jbyteArray) -> JniResult<Hash> {
    // TODO: Optimize copying and allocations.
    let bytes = env.convert_byte_array(array)?;
    Ok(Hash::from_slice(&bytes).expect(
        "Unable to create `Hash` from the slice",
    ))
}

// Converts `Hash` to Java byte array.
pub fn convert_hash(env: &JNIEnv, hash: &Hash) -> JniResult<jbyteArray> {
    env.byte_array_from_slice(hash.as_ref())
}

/// Converts JNI `JString` into Rust `String`
pub fn convert_to_string<'e, V>(env: &JNIEnv<'e>, val: V) -> JniResult<String>
where
    V: Into<JString<'e>>,
{
    Ok(env.get_string(val.into())?.into())
}
