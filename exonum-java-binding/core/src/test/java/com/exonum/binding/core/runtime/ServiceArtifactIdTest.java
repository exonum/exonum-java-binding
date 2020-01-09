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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceArtifactIdTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "0:land-registry:v1",
      "1:com.acme/foo-service:1.1.1-beta1",
      "100500:foo:bar",
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
    String version = "0.1";
    ServiceArtifactId id = ServiceArtifactId.valueOf(runtimeId, name, version);

    assertThat(id.getRuntimeId()).isEqualTo(runtimeId);
    assertThat(id.getName()).isEqualTo(name);
    assertThat(id.getVersion()).isEqualTo(version);
  }

  @Test
  void newJavaId() {
    String name = "test-service";
    String version = "test-version";
    ServiceArtifactId id = ServiceArtifactId.newJavaId(name, version);

    assertThat(id.getRuntimeId()).isEqualTo(RuntimeId.JAVA.getId());
    assertThat(id.getName()).isEqualTo(name);
  }

  @Test
  void toStringTest() {
    ServiceArtifactId id = ServiceArtifactId.valueOf(0, "full-name", "0.1");

    assertThat(id.toString()).isEqualTo("0:full-name:0.1");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "too-few:components:1.0",
      "com.acme:foo-service:0.1.0:extra-component",
      " : : ",
      "com acme:foo:1.0",
      "com.acme:foo service:1.0",
      "com.acme:foo-service:1 0",
      "com.acme:foo-service: 1.0",
      "com.acme:foo-service:1.0 ",
  })
  void checkInvalidName(String artifactId) {
    assertThrows(IllegalArgumentException.class,
        () -> ServiceArtifactId.parseFrom(artifactId));
  }
}
