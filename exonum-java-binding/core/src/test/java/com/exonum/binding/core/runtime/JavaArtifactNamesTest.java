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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JavaArtifactNamesTest {

  @Test
  void checkValidName() {
    String name = "com.acme:foo:1.0";
    JavaArtifactNames.checkArtifactName(name);
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
  void checkInvalidName(String serviceId) {
    assertThrows(IllegalArgumentException.class,
        () -> JavaArtifactNames.checkArtifactName(serviceId));
  }
}
