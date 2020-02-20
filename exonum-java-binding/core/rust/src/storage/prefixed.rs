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

use exonum::merkledb::{
    access::Prefixed,
    generic::{ErasedAccess, GenericAccess},
};
use jni::{
    objects::{JObject, JString},
    JNIEnv,
};

use std::panic;

use crate::{
    handle,
    utils::{convert_to_string, unwrap_exc_or_default},
    Handle,
};

/// Creates `Prefixed` for the given `namespace` from base access (passed as `access_handle`).
///
/// Throws exception and returns null if creation of `Prefixed` is not possible.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Prefixed_nativeCreate(
    env: JNIEnv,
    _: JObject,
    namespace: JString,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess<'static>>(access_handle);
        let namespace = convert_to_string(&env, namespace)?;
        let prefixed = match access {
            GenericAccess::Raw(raw) => Prefixed::new(namespace, raw.clone()),
            _ => panic!(
                "Attempt to create Prefixed from non-RawAccess: {:?}",
                access
            ),
        };
        let prefixed = GenericAccess::Prefixed(prefixed) as ErasedAccess;
        let handle = handle::to_handle(prefixed);
        Ok(handle)
    });

    unwrap_exc_or_default(&env, res)
}
