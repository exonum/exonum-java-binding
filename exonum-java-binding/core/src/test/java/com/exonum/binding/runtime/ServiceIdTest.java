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

package com.exonum.binding.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceIdTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "com.acme:foo-service:0.1.0",
      "com.acme:foo-service:1.1.1-beta1",
      "::",
  })
  void parseFromRoundtrip(String serviceId) {
    ServiceId parsedId = ServiceId.parseFrom(serviceId);
    String parsedAsString = parsedId.toString();

    assertThat(parsedAsString).isEqualTo(serviceId);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "too-few:components",
      "com.acme:foo-service:0.1.0:extra-component",
      " : : ",
      "com acme:foo:1.0",
      "com.acme:foo service:1.0",
      "com.acme:foo-service:1 0",
      "com.acme:foo-service: 1.0",
      "com.acme:foo-service:1.0 ",
  })
  void parseFromInvalidInput(String serviceId) {
    assertThrows(IllegalArgumentException.class, () -> ServiceId.parseFrom(serviceId));
  }

  @Test
  void of() {
    String groupId = "com.acme";
    String artifactId = "foo";
    String version = "1.0";
    ServiceId id = ServiceId.of(groupId, artifactId, version);

    assertThat(id.getGroupId()).isEqualTo(groupId);
    assertThat(id.getArtifactId()).isEqualTo(artifactId);
    assertThat(id.getVersion()).isEqualTo(version);
  }

  @Test
  void ofRejectsNulls() {
    new NullPointerTester()
        .setDefault(String.class, "foo")
        .testStaticMethods(ServiceId.class, Visibility.PACKAGE);
  }

  @ParameterizedTest
  @CsvSource({
      "com acme,foo,1.0",
      "com.acme,f o,1.0",
      "com.acme,foo,1 0",
      "' com.acme',foo,1.0",
      "'com.acme ',foo,1.0",
      "com:acme,foo,1.0",
  })
  void ofInvalidComponents(String groupId, String artifactId, String version) {
    assertThrows(IllegalArgumentException.class, () -> ServiceId.of(groupId, artifactId, version));
  }

  @Test
  void toStringTest() {
    String groupId = "com.acme";
    String artifactId = "foo";
    String version = "1.0";
    ServiceId id = ServiceId.of(groupId, artifactId, version);

    assertThat(id.toString()).isEqualTo("com.acme:foo:1.0");
  }
}
