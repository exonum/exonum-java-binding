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
import com.exonum.binding.testkit.*;
import com.exonum.binding.time.TimeSchema;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@RequiresNativeLibrary
class TimeSchemaProxyIntegrationTest {

  private static final ZonedDateTime EXPECTED_TIME = ZonedDateTime
      .of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
  private TimeProvider timeProvider = FakeTimeProvider.create(EXPECTED_TIME);

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withService(TestServiceModule.class)
          .withTimeService(timeProvider));

  @Test
  void getTime(TestKit testKit) {
    setUpConsolidatedTime(testKit);
    testKitTest(testKit, (timeSchema) -> {
      Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
      assertThat(consolidatedTime).hasValue(EXPECTED_TIME);
    });
  }

  @Test
  void getTimeBeforeConsolidated(TestKit testKit) {
    testKitTest(testKit, (timeSchema) -> {
      Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
      assertThat(consolidatedTime).isEmpty();
    });
  }

  @Test
  void getValidatorsTime(TestKit testKit) {
    setUpConsolidatedTime(testKit);
    testKitTest(testKit, (timeSchema) -> {
      Map<PublicKey, ZonedDateTime> validatorsTimes = toMap(timeSchema.getValidatorsTimes());
      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      PublicKey nodePublicKey = emulatedNode.getServiceKeyPair().getPublicKey();
      Map<PublicKey, ZonedDateTime> expected = ImmutableMap.of(nodePublicKey, EXPECTED_TIME);
      assertThat(validatorsTimes).isEqualTo(expected);
    });
  }

  private void setUpConsolidatedTime(TestKit testKit) {
    // Commit two blocks for time oracle to update consolidated time. Two blocks are needed as
    // after the first block time transactions are generated and after the second one they are
    // processed
    testKit.createBlock();
    testKit.createBlock();
  }

  private void testKitTest(TestKit testKit, Consumer<TimeSchema> test) {
    testKit.withSnapshot((view) -> {
      TimeSchema timeSchema = TimeSchema.newInstance(view);
      test.accept(timeSchema);

      return null;
    });
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }
}
