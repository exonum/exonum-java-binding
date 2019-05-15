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

package com.exonum.binding.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.EmulatedNodeType;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TimeProvider;
import com.exonum.binding.time.TimeSchema;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class TimeSchemaProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  private static final ZonedDateTime EXPECTED_TIME = ZonedDateTime
      .of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
  private TestKit testKit;

  @BeforeEach
  void setUpConsolidatedTime() {
    TimeProvider timeProvider = FakeTimeProvider.create(EXPECTED_TIME);
    testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withTimeService(timeProvider)
        .build();

    // Commit two blocks for time oracle to update consolidated time. Two blocks are needed as
    // after the first block time transactions are generated and after the second one they are
    // processed
    testKit.createBlock();
    testKit.createBlock();
  }

  @AfterEach
  void destroyTestkit() {
    testKit.close();
  }

  @Test
  void getTime() {
    testKit.withSnapshot((view) -> {
      TimeSchema timeSchema = TimeSchema.newInstance(view);
      Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
      assertThat(consolidatedTime).hasValue(EXPECTED_TIME);
      return null;
    });
  }

  @Test
  void getValidatorsTime() {
    testKit.withSnapshot((view) -> {
      TimeSchema timeSchema = TimeSchema.newInstance(view);
      Map<PublicKey, ZonedDateTime> validatorsTimes = toMap(timeSchema.getValidatorsTimes());
      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      PublicKey nodePublicKey = emulatedNode.getServiceKeyPair().getPublicKey();
      Map<PublicKey, ZonedDateTime> expected = ImmutableMap.of(nodePublicKey, EXPECTED_TIME);
      assertThat(validatorsTimes).isEqualTo(expected);
      return null;
    });
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }
}
