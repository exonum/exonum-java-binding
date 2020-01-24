/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.time;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.runtime.DispatcherSchema;
import com.exonum.binding.core.runtime.RuntimeId;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.core.messages.Runtime.ArtifactId;
import com.exonum.core.messages.Runtime.InstanceSpec;
import com.exonum.core.messages.Runtime.InstanceState;
import java.time.ZonedDateTime;

class TimeSchemaProxy implements TimeSchema {

  private static final int RUST_RUNTIME_ID = RuntimeId.RUST.getId();
  private static final String EXONUM_TIME_ARTIFACT_NAME = "exonum-time";

  private static final Serializer<PublicKey> PUBLIC_KEY_SERIALIZER =
      StandardSerializers.publicKey();
  private static final Serializer<ZonedDateTime> ZONED_DATE_TIME_SERIALIZER =
      UtcZonedDateTimeSerializer.INSTANCE;

  private final Access access;
  private final String name;

  TimeSchemaProxy(Access access, String name) {
    this.name = name;
    this.access = access;
    checkIfEnabled();
  }

  private void checkIfEnabled() {
    MapIndex<String, InstanceState> serviceInstances =
        new DispatcherSchema(access).serviceInstances();
    checkArgument(serviceInstances.containsKey(name), "No time service instance "
        + "with the given name (%s) started.", name);

    // TODO(ECR-3953): check instance status
    InstanceSpec serviceSpec = serviceInstances.get(name).getSpec();
    ArtifactId artifactId = serviceSpec.getArtifact();
    checkArgument(isTimeOracleInstance(artifactId), "Service with the given name (%s) is not "
        + "an Exonum time oracle, but %s.", name, artifactId);
  }

  private static boolean isTimeOracleInstance(ArtifactId artifactId) {
    return artifactId.getRuntimeId() == RUST_RUNTIME_ID
        && artifactId.getName().equals(EXONUM_TIME_ARTIFACT_NAME);
  }

  @Override
  public ProofEntryIndexProxy<ZonedDateTime> getTime() {
    IndexAddress address = indexAddress(TimeIndex.TIME);
    return access.getProofEntry(address, ZONED_DATE_TIME_SERIALIZER);
  }

  @Override
  public ProofMapIndexProxy<PublicKey, ZonedDateTime> getValidatorsTimes() {
    IndexAddress address = indexAddress(TimeIndex.VALIDATORS_TIMES);
    return access.getRawProofMap(address, PUBLIC_KEY_SERIALIZER, ZONED_DATE_TIME_SERIALIZER);
  }

  private IndexAddress indexAddress(String simpleName) {
    return IndexAddress.valueOf(name + "." + simpleName);
  }

  /**
   * Mapping for Exonum time indexes by name.
   */
  private static final class TimeIndex {
    private static final String VALIDATORS_TIMES = "validators_times";
    private static final String TIME = "time";
  }
}
