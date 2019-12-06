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

import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_DIR;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_FILENAME;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TimeProvider;
import com.exonum.binding.time.TimeSchema;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@RequiresNativeLibrary
class TimeSchemaProxyIntegrationTest {

  private static final ZonedDateTime EXPECTED_TIME = ZonedDateTime
      .of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

  @Test
  void newInstanceFailsIfNoSuchService(@TempDir Path tmp) {
    try (TestKit testkit = TestKit.builder()
        .withArtifactsDirectory(tmp)
        .build()) {
      Snapshot snapshot = testkit.getSnapshot();
      String timeServiceName = "inactive-service";
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> TimeSchema.newInstance(snapshot, timeServiceName));

      assertThat(e.getMessage()).containsIgnoringCase("No time service instance")
          .contains(timeServiceName);
    }
  }

  @Test
  void newInstanceFailsIfServiceOfOtherType() {
    String serviceName = "inactive-service";
    try (TestKit testkit = TestKit.builder()
        .withArtifactsDirectory(ARTIFACT_DIR)
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, serviceName, 10)
        .build()) {
      Snapshot snapshot = testkit.getSnapshot();
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> TimeSchema.newInstance(snapshot, serviceName));

      assertThat(e.getMessage()).containsIgnoringCase("Not an Exonum time oracle")
          .contains(serviceName)
          .contains(ARTIFACT_ID.getName());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "default-time",
      "mars-time"
  })
  void getTimeAccessibleFromAnyInstance(String timeServiceName, @TempDir Path tmp) {
    TimeProvider timeProvider = FakeTimeProvider.create(EXPECTED_TIME);
    try (TestKit testkit = TestKit.builder()
        .withArtifactsDirectory(tmp)
        .withTimeService(timeServiceName, 10, timeProvider)
        .build()) {
      setUpConsolidatedTime(testkit);
      Snapshot snapshot = testkit.getSnapshot();
      TimeSchema timeSchema = TimeSchema.newInstance(snapshot, timeServiceName);
      assertThat(timeSchema.getTime().toOptional()).hasValue(EXPECTED_TIME);
    }
  }

  @Nested
  class WithTimeService {

    private static final String SERVICE_NAME = "default-time";
    private TestKit testKit;

    @BeforeEach
    void createTestKit(@TempDir Path tmp) {
      TimeProvider timeProvider = FakeTimeProvider.create(EXPECTED_TIME);
      testKit = TestKit.builder()
          .withArtifactsDirectory(tmp)
          .withTimeService(SERVICE_NAME, 10, timeProvider)
          .build();
    }

    @AfterEach
    void destroyTestkit() {
      testKit.close();
    }

    @Test
    void getTime() {
      setUpConsolidatedTime();
      testKitTest((timeSchema) -> {
        Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
        assertThat(consolidatedTime).hasValue(EXPECTED_TIME);
      });
    }

    @Test
    void getTimeBeforeConsolidated() {
      testKitTest((timeSchema) -> {
        Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
        assertThat(consolidatedTime).isEmpty();
      });
    }

    @Test
    void getValidatorsTime() {
      setUpConsolidatedTime();
      testKitTest((timeSchema) -> {
        Map<PublicKey, ZonedDateTime> validatorsTimes = toMap(timeSchema.getValidatorsTimes());
        EmulatedNode emulatedNode = testKit.getEmulatedNode();
        PublicKey nodePublicKey = emulatedNode.getServiceKeyPair().getPublicKey();
        Map<PublicKey, ZonedDateTime> expected = ImmutableMap.of(nodePublicKey, EXPECTED_TIME);
        assertThat(validatorsTimes).isEqualTo(expected);
      });
    }

    private void setUpConsolidatedTime() {
      TimeSchemaProxyIntegrationTest.setUpConsolidatedTime(testKit);
    }

    private void testKitTest(Consumer<TimeSchema> test) {
      Snapshot view = testKit.getSnapshot();
      TimeSchema timeSchema = TimeSchema.newInstance(view, SERVICE_NAME);
      test.accept(timeSchema);
    }
  }

  static void setUpConsolidatedTime(TestKit testKit) {
    // Commit two blocks for time oracle to update consolidated time. Two blocks are needed as
    // after the first block time transactions are generated and after the second one they are
    // processed
    testKit.createBlock();
    testKit.createBlock();
  }

  private static <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }
}
