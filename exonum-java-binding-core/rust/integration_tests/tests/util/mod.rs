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

use jni::{InitArgsBuilder, JNIVersion, JavaVM};

/// Creates a configured instance of `JavaVM`.
/// _This function should be called only *once*._
pub fn create_vm(debug: bool) -> JavaVM {
    let mut jvm_args_builder = InitArgsBuilder::new().version(JNIVersion::V8);

    if debug {
        jvm_args_builder = jvm_args_builder.option("-Xcheck:jni").option("-Xdebug");
    }

    let jvm_args = jvm_args_builder.build().unwrap_or_else(
        |e| panic!(format!("{:#?}", e)),
    );

    JavaVM::new(jvm_args).unwrap_or_else(|e| panic!(format!("{:#?}", e)))
}
