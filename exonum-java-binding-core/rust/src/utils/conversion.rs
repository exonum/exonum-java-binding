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

use jni::JNIEnv;
use jni::sys::jbyteArray;
use jni::errors::Result;
use jni::objects::JString;

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

pub fn convert_to_string(env: &JNIEnv, val: JString) -> Result<String> {
    Ok(env.get_string(val)?.into())
}
