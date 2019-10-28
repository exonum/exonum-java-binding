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

package com.exonum.binding.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceArtifactIdTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "0:land-registry",
      "1:com.acme:foo-service:1.1.1-beta1",
      "100500:::",
  })
  void parseFromRoundtrip(String serviceId) {
    ServiceArtifactId parsedId = ServiceArtifactId.parseFrom(serviceId);
    String parsedAsString = parsedId.toString();

    assertThat(parsedAsString).isEqualTo(serviceId);
  }

  @Test
  void valueOf() {
    int runtimeId = 1;
    String name = "test-service";
    ServiceArtifactId id = ServiceArtifactId.valueOf(runtimeId, name);

    assertThat(id.getRuntimeId()).isEqualTo(runtimeId);
    assertThat(id.getName()).isEqualTo(name);
  }

  @Test
  void newJavaId() {
    String name = "test-service";
    ServiceArtifactId id = ServiceArtifactId.newJavaId(name);

    assertThat(id.getRuntimeId()).isEqualTo(RuntimeId.JAVA.getId());
    assertThat(id.getName()).isEqualTo(name);
  }

  @Test
  void toStringTest() {
    ServiceArtifactId id = ServiceArtifactId.valueOf(0, "full-name");

    assertThat(id.toString()).isEqualTo("0:full-name");
  }
}
