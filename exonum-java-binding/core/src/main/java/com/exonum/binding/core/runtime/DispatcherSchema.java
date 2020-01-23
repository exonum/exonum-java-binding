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

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.core.messages.Runtime.InstanceState;

/** Exonum service instances database schema. */
public class DispatcherSchema {

  private static final IndexAddress DISPATCHER_INSTANCES =
      IndexAddress.valueOf("dispatcher_instances");
  private static final Serializer<InstanceState> INSTANCE_STATE_SERIALIZER =
      protobuf(InstanceState.class);

  private final Access access;

  public DispatcherSchema(Access access) {
    this.access = access;
  }

  /**
   * Returns a map of service instance specifications of started services indexed by their names.
   */
  public ProofMapIndexProxy<String, InstanceState> serviceInstances() {
    return access.getProofMap(DISPATCHER_INSTANCES, string(), INSTANCE_STATE_SERIALIZER);
  }
}
