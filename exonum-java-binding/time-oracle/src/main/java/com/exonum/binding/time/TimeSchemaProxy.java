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
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.messages.Runtime.ArtifactId;
import com.exonum.binding.messages.Runtime.InstanceSpec;
import java.time.ZonedDateTime;

class TimeSchemaProxy implements TimeSchema {

  private static final int RUST_RUNTIME_ID = 0;
  private static final String EXONUM_TIME_ARTIFACT_NAME_PREFIX = "exonum-time:";

  private static final Serializer<PublicKey> PUBLIC_KEY_SERIALIZER =
      StandardSerializers.publicKey();
  private static final Serializer<ZonedDateTime> ZONED_DATE_TIME_SERIALIZER =
      UtcZonedDateTimeSerializer.INSTANCE;

  private final View view;
  private final String name;

  TimeSchemaProxy(View view, String name) {
    this.name = name;
    this.view = view;
    checkIfEnabled();
  }

  private void checkIfEnabled() {
    MapIndex<String, InstanceSpec> serviceInstances = new RuntimeSchema(view).serviceInstances();
    checkArgument(serviceInstances.containsKey(name), "No service instance "
        + "with the given name (%s) started.", name);

    InstanceSpec serviceSpec = serviceInstances.get(name);
    ArtifactId artifactId = serviceSpec.getArtifact();
    checkArgument(isTimeOracleInstance(artifactId), "Service with the given name (%s) is not "
        + "an Exonum time oracle, but %s.", name, artifactId);
  }

  private static boolean isTimeOracleInstance(ArtifactId artifactId) {
    return artifactId.getRuntimeId() == RUST_RUNTIME_ID
        && artifactId.getName().startsWith(EXONUM_TIME_ARTIFACT_NAME_PREFIX);
  }

  @Override
  public EntryIndexProxy<ZonedDateTime> getTime() {
    return EntryIndexProxy.newInstance(indexName(TimeIndex.TIME), view, ZONED_DATE_TIME_SERIALIZER);
  }

  @Override
  public ProofMapIndexProxy<PublicKey, ZonedDateTime> getValidatorsTimes() {
    return ProofMapIndexProxy.newInstance(indexName(TimeIndex.VALIDATORS_TIMES), view,
        PUBLIC_KEY_SERIALIZER, ZONED_DATE_TIME_SERIALIZER);
  }

  private String indexName(String simpleName) {
    return name + "." + simpleName;
  }

  /**
   * Mapping for Exonum time indexes by name.
   */
  private static final class TimeIndex {
    private static final String VALIDATORS_TIMES = "validators_times";
    private static final String TIME = "time";
  }
}
