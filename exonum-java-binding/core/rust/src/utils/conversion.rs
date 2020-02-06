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

use exonum::{crypto::Hash, merkledb::IndexAddress};
use exonum_proto::ProtobufConvert;
use jni::objects::JString;
use jni::sys::{jbyteArray, jobjectArray};
use jni::JNIEnv;
use protobuf::Message;

use std::ptr;

use JniResult;

/// Converts Java byte array to `Hash`. Panics if array has the wrong length.
pub fn convert_to_hash(env: &JNIEnv, array: jbyteArray) -> JniResult<Hash> {
    let bytes = env.convert_byte_array(array)?;
    Ok(Hash::from_slice(&bytes).expect("Unable to create `Hash` from the slice"))
}

/// Converts `Hash` to Java byte array.
pub fn convert_hash(env: &JNIEnv, hash: &Hash) -> JniResult<jbyteArray> {
    env.byte_array_from_slice(hash.as_ref())
}

// todo: @bogdanov — please rewrite appropriately
/// Converts a pair of (name, @Nullable id_in_group) into Rust IndexAddress.
pub fn convert_to_index_address<'e, S>(
    env: &JNIEnv<'e>,
    name: S,
    id_in_group: jbyteArray,
) -> JniResult<IndexAddress>
where
    S: Into<JString<'e>>,
{
    let name = convert_to_string(env, name)?;
    let address = IndexAddress::from_root(name);
    if id_in_group.is_null() {
        // Name address
        Ok(address)
    } else {
        // Address in group
        let id_in_group = env.convert_byte_array(id_in_group)?;
        Ok(address.append_key(&id_in_group))
    }
}

/// Converts JNI `JString` into Rust `String`.
pub fn convert_to_string<'e, V>(env: &JNIEnv<'e>, val: V) -> JniResult<String>
where
    V: Into<JString<'e>>,
{
    Ok(env.get_string(val.into())?.into())
}

/// Converts anything convertible to a protobuf message into Java byte array.
pub fn proto_to_java_bytes<P: ProtobufConvert<ProtoStruct = impl Message>>(
    env: &JNIEnv,
    proto: &P,
) -> JniResult<jbyteArray> {
    let bytes = proto.to_pb().write_to_bytes().unwrap();
    env.byte_array_from_slice(&bytes)
}

/// Converts array of Java bytes arrays (`byte[][]`) to the vector of Rust array representation.
pub fn java_arrays_to_rust<T, F>(
    env: &JNIEnv,
    array: jobjectArray,
    to_rust_array: F,
) -> JniResult<Vec<T>>
where
    F: Fn(&JNIEnv, jbyteArray) -> JniResult<T>,
{
    let num_elements = env.get_array_length(array)?;
    let mut result = Vec::with_capacity(num_elements as usize);
    for i in 0..num_elements {
        let array_element = env.auto_local(env.get_object_array_element(array, i)?);
        let array = to_rust_array(&env, array_element.as_obj().into_inner())?;
        result.push(array);
    }
    Ok(result)
}

/// Converts optional array of bytes into `jbyteArray`.
///
/// If `None` passed, returns null.
pub fn optional_array_to_java<B: AsRef<[u8]>>(
    env: &JNIEnv,
    slice: Option<B>,
) -> JniResult<jbyteArray> {
    slice.map_or(Ok(ptr::null_mut() as *mut _), |slice| {
        env.byte_array_from_slice(slice.as_ref())
    })
}
