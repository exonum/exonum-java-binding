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

import com.google.common.base.MoreObjects;
import java.util.Optional;

/**
 * A base class for migration scripts implementation.
 */
public abstract class AbstractMigrationScript implements MigrationScript {

  @Override
  public Optional<String> minSupportedVersion() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("MigrationScript")
        .add("name", name())
        .add("targetVersion", targetVersion())
        .add("minSupportedVersion", minSupportedVersion())
        .toString();
  }
}
