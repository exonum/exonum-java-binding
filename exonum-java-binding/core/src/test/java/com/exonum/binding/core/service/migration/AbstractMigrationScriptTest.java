/*
 * Copyright 2020 The Exonum Team
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

package com.exonum.binding.core.service.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.core.service.ExecutionContext;
import org.junit.jupiter.api.Test;

class AbstractMigrationScriptTest {
  private final AbstractMigrationScript script = new ScriptUnderTest();

  @Test
  void minSupportedVersion() {
    assertThat(script.minSupportedVersion()).isEmpty();
  }

  @Test
  void testToString() {
    assertThat(script.toString())
        .contains(script.name())
        .contains(script.targetVersion())
        .contains(script.minSupportedVersion().toString());
  }

  static class ScriptUnderTest extends AbstractMigrationScript {
    @Override
    public String name() {
      return "test script";
    }

    @Override
    public String targetVersion() {
      return "0.0.1";
    }

    @Override
    public void execute(ExecutionContext context) {
      //no-op
    }
  }
}
