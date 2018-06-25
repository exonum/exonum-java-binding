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

use exonum::crypto::Hash;
use jni::objects::JString;
use jni::sys::jbyteArray;
use jni::JNIEnv;

use JniResult;

// Converts Java byte array to `Hash`. Panics if array has the wrong length.
pub fn convert_to_hash(env: &JNIEnv, array: jbyteArray) -> JniResult<Hash> {
    // TODO: Optimize copying and allocations.
    let bytes = env.convert_byte_array(array)?;
    Ok(Hash::from_slice(&bytes).expect("Unable to create `Hash` from the slice"))
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
