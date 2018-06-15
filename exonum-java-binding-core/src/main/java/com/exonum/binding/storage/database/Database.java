/* 
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseableNativeProxy;

/**
 * Represents an underlying Exonum Storage database.
 */
public interface Database extends CloseableNativeProxy {

  /**
   * Creates a new snapshot of the database state.
   *
   * @param cleaner a cleaner to register the snapshot
   * @return a new snapshot of the database state
   */
  Snapshot createSnapshot(Cleaner cleaner);

  /**
   * Creates a new database fork.
   *
   * <p>A fork allows to perform a transaction: a number of independent writes to a database,
   * which then may be <em>atomically</em> applied to the database.
   *
   * @param cleaner a cleaner to register the fork
   * @return a new database fork
   */
  Fork createFork(Cleaner cleaner);
}
