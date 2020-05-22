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

package com.exonum.binding.core.runtime;

import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.service.migration.MigrationScript;

public class TestMigrationScript1 implements MigrationScript {
  @Override
  public String name() {
    return "Test migration 1";
  }

  @Override
  public String targetVersion() {
    return "1.0";
  }

  @Override
  public void execute(ExecutionContext context) {
    // no-op
  }
}
