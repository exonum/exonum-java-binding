/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.common.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.common.configuration.Consensus;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.configuration.ValidatorKey;
import com.exonum.binding.common.hash.HashCode;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class StoredConfigurationGsonSerializerTest {

  private StoredConfigurationGsonSerializer serializer = new StoredConfigurationGsonSerializer();

  @Test
  void roundTripTest() {
    StoredConfiguration configuration = createConfiguration();
    StoredConfiguration restoredConfiguration = serializer
        .fromJson(serializer.toJson(configuration));

    assertThat(restoredConfiguration, equalTo(configuration));
  }

  private StoredConfiguration createConfiguration() {
    return StoredConfiguration.create(
        HashCode.fromString("11"),
        1,
        Collections.singletonList(
            ValidatorKey.create(HashCode.fromString("22"), HashCode.fromString("33"))
        ),
        Consensus.create(
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8)
    );
  }
}
