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
use jni::objects::{GlobalRef, JMethodID};

use std::mem;

use JniResult;

pub struct PairIter<InnerIter: Iterator> {
    pub iter: InnerIter,
    pub element_class: GlobalRef,
    pub constructor_id: JMethodID<'static>,
}

impl<InnerIter: Iterator> PairIter<InnerIter> {
    pub fn new(env: &JNIEnv, iter: InnerIter, class_name: &str) -> JniResult<Self> {
        let class = env.find_class(class_name)?;
        let element_class = env.new_global_ref(class.into())?;
        let signature = "([B[B)V";
        let id = env.get_method_id(class_name, "<init>", signature)?;
        Ok(PairIter {
            iter,
            element_class,
            constructor_id: unsafe { mem::transmute(id) },
        })
    }
}
