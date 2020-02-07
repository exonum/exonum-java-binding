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
import static java.lang.String.format;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.runtime.RuntimeId;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.messages.core.runtime.Base.ArtifactId;
import com.exonum.messages.core.runtime.Base.InstanceSpec;
import com.exonum.messages.core.runtime.Lifecycle.InstanceState;
import java.time.ZonedDateTime;

class TimeSchemaProxy implements TimeSchema {

  private static final int RUST_RUNTIME_ID = RuntimeId.RUST.getId();
  private static final String EXONUM_TIME_ARTIFACT_NAME = "exonum-time";

  private static final Serializer<PublicKey> PUBLIC_KEY_SERIALIZER =
      StandardSerializers.publicKey();
  private static final Serializer<ZonedDateTime> ZONED_DATE_TIME_SERIALIZER =
      UtcZonedDateTimeSerializer.INSTANCE;

  private final Prefixed access;

  TimeSchemaProxy(BlockchainData blockchainData, String name) {
    this.access = getTimeOracleDbAccess(blockchainData, name);
  }

  private static Prefixed getTimeOracleDbAccess(BlockchainData blockchainData, String name) {
    MapIndex<String, InstanceState> serviceInstances = blockchainData.getDispatcherSchema()
        .serviceInstances();
    checkArgument(serviceInstances.containsKey(name), "No time service instance "
        + "with the given name (%s) started.", name);

    // TODO(ECR-3953): check instance status
    InstanceSpec serviceSpec = serviceInstances.get(name).getSpec();
    ArtifactId artifactId = serviceSpec.getArtifact();
    checkArgument(isTimeOracleInstance(artifactId), "Service with the given name (%s) is not "
        + "an Exonum time oracle, but %s.", name, artifactId);

    return blockchainData.findServiceData(name)
        .orElseThrow(
            () -> new IllegalArgumentException(format("Cannot access service %s data", name)));
  }

  private static boolean isTimeOracleInstance(ArtifactId artifactId) {
    return artifactId.getRuntimeId() == RUST_RUNTIME_ID
        && artifactId.getName().equals(EXONUM_TIME_ARTIFACT_NAME);
  }

  @Override
  public ProofEntryIndexProxy<ZonedDateTime> getTime() {
    return access.getProofEntry(TimeIndex.TIME, ZONED_DATE_TIME_SERIALIZER);
  }

  @Override
  public ProofMapIndexProxy<PublicKey, ZonedDateTime> getValidatorsTimes() {
    return access.getRawProofMap(TimeIndex.VALIDATORS_TIMES, PUBLIC_KEY_SERIALIZER,
        ZONED_DATE_TIME_SERIALIZER);
  }

  /**
   * Mapping for Exonum time indexes by address.
   */
  private static final class TimeIndex {
    private static final IndexAddress VALIDATORS_TIMES = IndexAddress.valueOf("validators_times");
    private static final IndexAddress TIME = IndexAddress.valueOf("time");
  }
}
