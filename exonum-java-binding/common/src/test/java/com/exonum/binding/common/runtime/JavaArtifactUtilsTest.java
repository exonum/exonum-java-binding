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

import static com.exonum.binding.common.runtime.JavaArtifactUtils.checkNoForbiddenChars;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JavaArtifactUtilsTest {

  @Test
  void checkArtifactWithNoForbiddenCharacters() {
    String name = "com.acme/foo:1.0";
    checkNoForbiddenChars(name);
  }

  @Test
  void checkArtifactWithForbiddenCharacters() {
    String name = "com.acme foo:1.0";
    assertThrows(IllegalArgumentException.class, () -> checkNoForbiddenChars(name));
  }
}
