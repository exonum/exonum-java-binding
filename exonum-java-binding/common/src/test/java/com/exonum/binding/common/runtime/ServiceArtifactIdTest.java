/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.messages.core.runtime.Base.ArtifactId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceArtifactIdTest {
  private static final ArtifactId ARTIFACT_ID =
      ArtifactId.newBuilder()
          .setRuntimeId(1)
          .setName("com.acme/foo")
          .setVersion("1.2.3")
          .build();

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
  void fromProto() {
    ArtifactId artifactId = ARTIFACT_ID;

    ServiceArtifactId serviceArtifactId = ServiceArtifactId.fromProto(artifactId);

    assertThat(serviceArtifactId.getRuntimeId()).isEqualTo(artifactId.getRuntimeId());
    assertThat(serviceArtifactId.getName()).isEqualTo(artifactId.getName());
    assertThat(serviceArtifactId.getVersion()).isEqualTo(artifactId.getVersion());
  }

  @Test
  void toStringTest() {
    ServiceArtifactId id = ServiceArtifactId.valueOf(0, "full-name", "0.1");

    assertThat(id.toString()).isEqualTo("0:full-name:0.1");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      /* Too few components */
      "too-few-components",
      "1:too-few-components",
      /* Extra component */
      "1:foo-service:0.1.0:extra-component",
      /* All blanks */
      " : : ",
      /* Non-integral runtime id */
      "a:com.acme/foo:1.0",
      /* Spaces in runtime id */
      "1 :com.acme/foo:1.0",
      /* Spaces in name */
      "1:com.acme foo:1.0",
      "1:com.acme/fo o:1.0",
      /* Spaces in version */
      "1:com.acme:foo: 1.0",
      "1:com.acme/foo:1.0 ",
      "1:com.acme:foo:1 0",
  })
  void checkInvalidName(String artifactId) {
    assertThrows(IllegalArgumentException.class,
        () -> ServiceArtifactId.parseFrom(artifactId));
  }

}
