/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use chrono::{DateTime, Utc};
use exonum::storage::StorageValue;
use exonum_time::time_provider::TimeProvider;
use jni::objects::{GlobalRef, JObject};
use jni::JNIEnv;
use proxy::{JniExecutor, MainExecutor};
use std::fmt::Debug;
use utils::unwrap_jni;

//com.exonum.binding.testkit.TimeProvider

pub struct JavaTimeProvider {
    provider: GlobalRef,
    exec: MainExecutor,
}

impl Debug for JavaTimeProvider {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "JavaTimeProvider")
    }
}

impl JavaTimeProvider {
    pub fn new(exec: MainExecutor, java_provider: JObject) -> Self {
        let provider =
            unwrap_jni(exec.with_attached(|env: &JNIEnv| env.new_global_ref(java_provider)));
        Self { provider, exec }
    }
}

impl TimeProvider for JavaTimeProvider {
    fn current_time(&self) -> DateTime<Utc> {
        unwrap_jni(self.exec.with_attached(|env: &JNIEnv| {
            let java_date_time = env.call_method(
                self.provider.as_obj(),
                "getTime",
                "()Ljava/time/ZonedDateTime;",
                &[],
            )?;
            let serializer = env
                .get_static_field(
                    "com/exonum/binding/time/UtcZonedDateTimeSerializer",
                    "INSTANCE",
                    "Lcom/exonum/binding/time/UtcZonedDateTimeSerializer;",
                )?
                .l()?;
            let serialized_date_time = env
                .call_method(
                    serializer,
                    "toBytes",
                    "(Ljava/time/ZonedDateTime;)[B",
                    &[java_date_time],
                )?
                .l()?
                .into_inner();
            let serialized_date_time = env.convert_byte_array(serialized_date_time.into())?;
            let date_time = DateTime::from_bytes(serialized_date_time.into());

            Ok(date_time)
        }))
    }
}
