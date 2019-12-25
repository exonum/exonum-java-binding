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

package com.exonum.binding.core.runtime;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.common.serialization.StandardSerializers.string;

import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.core.messages.Runtime.InstanceState;

/**
 * Exonum service instances database schema.
 */
public class DispatcherSchema {
  private final View view;

  public DispatcherSchema(View view) {
    this.view = view;
  }

  /**
   * Returns a map of service instance specifications of started services indexed by their names.
   */
  public ProofMapIndexProxy<String, InstanceState> serviceInstances() {
    return ProofMapIndexProxy.newInstance("dispatcher_instances", view,
        string(), protobuf(InstanceState.class));
  }
}
