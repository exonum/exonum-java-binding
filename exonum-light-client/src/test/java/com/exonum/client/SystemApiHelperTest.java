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
 *
 */

package com.exonum.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.exonum.client.response.ConsensusStatus;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.SystemStatistics;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SystemApiHelperTest {

  @ParameterizedTest
  @MethodSource("healthCheckSource")
  void parseHealthCheckJson(String json, HealthCheckInfo expected) {
    HealthCheckInfo actual = SystemApiHelper.parseHealthCheckJson(json);
    assertThat(actual, is(expected));
  }

  @ParameterizedTest
  @MethodSource("statsSource")
  void parseStatsJson(String json, SystemStatistics expected) {
    SystemStatistics actual = SystemApiHelper.parseStatsJson(json);
    assertThat(actual, is(expected));
  }

  private static List<Arguments> statsSource() {
    return ImmutableList.of(
        arguments(formatStats(0, 0), new SystemStatistics(0, 0)),
        arguments(formatStats(1, 2), new SystemStatistics(1, 2)),
        // Check deserializer accepts longs-as-numbers (which is non-standard, but used by Exonum)
        arguments(
            formatStats(Integer.MAX_VALUE, Long.MAX_VALUE),
            new SystemStatistics(Integer.MAX_VALUE, Long.MAX_VALUE)),
        // and longs-as-strings (a common approach to represent longs)
        arguments(
            "{\"tx_pool_size\": " + 0 + ", \"tx_count\": \"" + Long.MAX_VALUE + "\"}",
            new SystemStatistics(0, Long.MAX_VALUE)));
  }

  private static String formatStats(int txPoolSize, long txCount) {
    return String.format("{\"tx_pool_size\": %d, \"tx_count\": %d}", txPoolSize, txCount);
  }

  private static List<Arguments> healthCheckSource() {
    return ImmutableList.of(
        arguments(
            "{\"consensus_status\": \"Enabled\", \"connected_peers\": 0}",
            new HealthCheckInfo(ConsensusStatus.ENABLED, 0)),
        arguments(
            "{\"consensus_status\": \"Disabled\", \"connected_peers\": 0}",
            new HealthCheckInfo(ConsensusStatus.DISABLED, 0)),
        arguments(
            "{\"consensus_status\": \"Active\", \"connected_peers\": 1 }",
            new HealthCheckInfo(ConsensusStatus.ACTIVE, 1)));
  }
}
