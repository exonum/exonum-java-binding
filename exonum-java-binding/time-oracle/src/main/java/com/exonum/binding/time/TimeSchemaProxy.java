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

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import java.time.ZonedDateTime;

class TimeSchemaProxy implements TimeSchema {

  private static final Serializer<PublicKey> PUBLIC_KEY_SERIALIZER =
      StandardSerializers.publicKey();
  private static final Serializer<ZonedDateTime> ZONED_DATE_TIME_SERIALIZER =
      UtcZonedDateTimeSerializer.INSTANCE;

  private final View dbView;

  TimeSchemaProxy(View dbView) {
    checkIfEnabled();
    this.dbView = dbView;
  }

  @Override
  public EntryIndexProxy<ZonedDateTime> getTime() {
    return EntryIndexProxy.newInstance(TimeIndex.TIME, dbView, ZONED_DATE_TIME_SERIALIZER);
  }

  @Override
  public ProofMapIndexProxy<PublicKey, ZonedDateTime> getValidatorsTimes() {
    return ProofMapIndexProxy.newInstance(TimeIndex.VALIDATORS_TIMES, dbView, PUBLIC_KEY_SERIALIZER,
        ZONED_DATE_TIME_SERIALIZER);
  }

  private void checkIfEnabled() {
    // Skip if invoked in tests because the check relies on the state unavailable
    // in integration tests. To be removed in ECR-2970 (Java Testkit)
    if (!runningUnitTests()) {
      checkState(isTimeServiceEnabled(), "Time service is not enabled. To enable it, put 'time' "
          + "into 'ejb_app_services.toml' file.");
    }
  }

  private static boolean runningUnitTests() {
    try {
      Class.forName("org.junit.jupiter.api.Test");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private native boolean isTimeServiceEnabled();

  /**
   * Mapping for Exonum time indexes by name.
   */
  private static final class TimeIndex {
    private static final String PREFIX = "exonum_time.";
    private static final String VALIDATORS_TIMES = PREFIX + "validators_times";
    private static final String TIME = PREFIX + "time";
  }
}
