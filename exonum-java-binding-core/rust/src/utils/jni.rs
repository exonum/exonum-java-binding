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

use jni::objects::JObject;
use jni::signature::JavaType;
use jni::JNIEnv;

use utils::{
    convert_to_string,
    jni_cache::{class, object, throwable},
};
use JniResult;

const RETVAL_TYPE_STRING: &str = "java/lang/String";
const RETVAL_TYPE_CLASS: &str = "java/lang/Class";

/// Returns a class name of an obj as a `String`.
pub fn get_class_name(env: &JNIEnv, obj: JObject) -> JniResult<String> {
    let class_object = unsafe {
        env.call_method_unsafe(
            obj,
            object::get_class_id(),
            JavaType::Object(RETVAL_TYPE_CLASS.into()),
            &[],
        )
    }?
    .l()?;

    let class_name = unsafe {
        env.call_method_unsafe(
            class_object,
            class::get_name_id(),
            JavaType::Object(RETVAL_TYPE_STRING.into()),
            &[],
        )
    }?
    .l()?;
    convert_to_string(env, class_name)
}

/// Returns the message from the exception if it is not null.
///
/// `exception` should extend `java.lang.Throwable` and be not null
pub fn get_exception_message(env: &JNIEnv, exception: JObject) -> JniResult<Option<String>> {
    assert!(!exception.is_null(), "Invalid exception argument");
    let message = unsafe {
        env.call_method_unsafe(
            exception,
            throwable::get_message_id(),
            JavaType::Object(RETVAL_TYPE_STRING.into()),
            &[],
        )
    }?;
    let message = message.l()?;
    if message.is_null() {
        return Ok(None);
    }
    convert_to_string(env, message).map(Some)
}
