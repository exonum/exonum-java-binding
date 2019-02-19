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

import com.exonum.client.response.ConsensusStatus;
import com.exonum.client.response.HealthCheckInfo;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SystemApiHelperTest {

  @ParameterizedTest
  @MethodSource("healthCheckSource")
  void healthCheckJsonParser(String json, HealthCheckInfo expected) {
    HealthCheckInfo actual = SystemApiHelper.healthCheckJsonParser(json);
    assertThat(actual, is(expected));
  }

  @ParameterizedTest
  @MethodSource("memoryPoolSource")
  void memoryPoolJsonParser(String json, int expected) {
    int actual = SystemApiHelper.memoryPoolJsonParser(json);
    assertThat(actual, is(expected));
  }

  private static List<Arguments> memoryPoolSource() {
    return ImmutableList.of(
        Arguments.of("{\"size\": 0}", 0),
        Arguments.of("{\"size\": 2}", 2),
        Arguments.of("{\"size\": " + Integer.MAX_VALUE + "}", Integer.MAX_VALUE)
    );
  }

  private static List<Arguments> healthCheckSource() {
    return ImmutableList.of(
        Arguments.of("{\"consensus_status\": \"Enabled\", \"connectivity\": \"NotConnected\"}",
            new HealthCheckInfo(ConsensusStatus.ENABLED, 0)),
        Arguments.of("{\"consensus_status\": \"Disabled\", \"connectivity\": \"NotConnected\"}",
            new HealthCheckInfo(ConsensusStatus.DISABLED, 0)),
        Arguments.of("{\"consensus_status\": \"Active\","
                + "\"connectivity\": {\"Connected\": {\"amount\": 1 } }"
                + "}",
            new HealthCheckInfo(ConsensusStatus.ACTIVE, 1))
    );
  }

}
