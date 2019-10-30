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

package com.exonum.binding.time;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.common.serialization.StandardSerializers.string;

import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.messages.Runtime.InstanceSpec;

// todo: Replace with dispatcher schema when testkit lands [ECR-3597]
class RuntimeSchema {

  private final View view;

  RuntimeSchema(View view) {
    this.view = view;
  }

  ProofMapIndexProxy<String, InstanceSpec> serviceInstances() {
    return ProofMapIndexProxy.newInstance("core.dispatcher.service_instances", view,
        string(), protobuf(InstanceSpec.class));
  }
}
