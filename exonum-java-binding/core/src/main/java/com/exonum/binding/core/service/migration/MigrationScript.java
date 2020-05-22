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

import com.exonum.binding.core.service.ExecutionContext;
import java.util.Optional;
import org.pf4j.ExtensionPoint;

/**
 * Migration script interface which allows to construct data migrations performed during service
 * evolution.
 *
 * <p/>
 * Migration script execution logic needs retain the knowledge about data types used in the service
 * in the past. Since these data types may be unused <b>currently</b>, retaining them may place
 * a burden on the service. To mitigate this, you can provide a minimum supported starting version
 * of the service via {@link #minSupportedVersion()}.
 *
 * <p/>
 * <h3>Contract and restrictions</h3>
 * Service artifact module could have any number of migration script implementations. To be visible
 * and applicable by the system those implementations must satisfy the following contract:
 *
 * <ul>
 *   <li>Migration script class should implement {@linkplain MigrationScript this interface}.</li>
 *   <li>Migration script class should have default constructor.</li>
 *   <li>Migration script class should be marked with {@link org.pf4j.Extension} annotation.</li>
 *   <li>There shouldn't be more two (or more) scripts for the same
 *   {@linkplain #targetVersion() target version} in the module.</li>
 * </ul>
 *
 * @see com.exonum.binding.core.service.migration
 * @see <a href="https://semver.org/">Semantic versioning</a>
 */
public interface MigrationScript extends ExtensionPoint {

  /**
   * Represents a name of the migration script.
   * I.e. short description of the migration purpose in a free form.
   */
  String name();

  /**
   * Minimum version of the service data the current script compatible with.
   * Or {@link Optional#empty()} if the script is compatible with any data version.
   */
  default Optional<String> minSupportedVersion() {
    return Optional.empty();
  }

  ;

  /**
   * Version of the service data the current script migrates to.
   */
  String targetVersion();

  //TODO: ECR-4413 replace ExecutionContext with MigrationContext

  /**
   * Performs data migration.
   *
   * @param context migration context
   */
  void execute(ExecutionContext context);

}
