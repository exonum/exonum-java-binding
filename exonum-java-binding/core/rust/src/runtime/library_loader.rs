// Copyright 2019 The Exonum Team
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

use jni::{objects::JClass, sys::jstring, JNIEnv};

/// Returns the current version of the library.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_util_LibraryLoader_nativeGetLibraryVersion(
    env: JNIEnv,
    _: JClass,
) -> jstring {
    env.new_string(get_lib_version()).unwrap().into_inner()
}

// Returns the exact value of the `version` field from the library's Cargo.toml configuration file.
fn get_lib_version() -> &'static str {
    env!("CARGO_PKG_VERSION")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    // This test could be useful in case cargo changed the way of defining the version of a package.
    pub fn check_version_is_not_empty() {
        let version = get_lib_version();
        assert!(!version.is_empty());
    }
}
