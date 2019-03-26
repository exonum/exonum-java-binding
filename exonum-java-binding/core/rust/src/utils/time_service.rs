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

use jni::objects::JClass;
use jni::sys::jboolean;
use jni::JNIEnv;

use utils::services::{
    is_service_enabled_in_config_file, system_service_names::TIME_SERVICE,
    PATH_TO_SERVICES_DEFINITION,
};

lazy_static! {
    static ref IS_TIME_SERVICE_ENABLED: jboolean =
        is_service_enabled_in_config_file(TIME_SERVICE, PATH_TO_SERVICES_DEFINITION).into();
}

#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_time_TimeSchemaProxy_isTimeServiceEnabled(
    _: JNIEnv,
    _: JClass,
) -> jboolean {
    // TODO: For the moment we're just checking for the service's name presence in the configuration
    // file. As soon as we have dynamic services implemented this checking should happen in runtime.
    // See https://jira.bf.local/browse/ECR-2893
    *IS_TIME_SERVICE_ENABLED
}
