/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.time;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import java.time.ZonedDateTime;

/**
 * Exonum time service database schema. It provides read-only access to the state
 * of a time oracle service instance.
 *
 * @see <a href="https://exonum.com/doc/version/0.12/advanced/time/">Time oracle documentation</a>
 */
public interface TimeSchema {

  /**
   * Constructs a schema of the time oracle instance with the given name.
   *
   * @param dbView the database view
   * @param name the name of the time oracle service instance to use
   *
   * @throws IllegalArgumentException if there is no service with the given name or it is not
   *     an Exonum time oracle
   */
  static TimeSchema newInstance(View dbView, String name) {
    return new TimeSchemaProxy(dbView, name);
  }

  /**
   * Returns consolidated time output by the service in UTC.
   *
   * <p>When this time oracle instance is started, the consolidated time remains unknown until
   * the transactions with time updates from at least two thirds of validator nodes are processed.
   * After that the time will be always present.
   */
  EntryIndexProxy<ZonedDateTime> getTime();

  /**
   * Returns the table that stores time for every validator. Note that this is a
   * <a href="ProofMapIndexProxy.html#key-hashing">proof map that uses non-hashed keys</a>.
   */
  ProofMapIndexProxy<PublicKey, ZonedDateTime> getValidatorsTimes();
}
