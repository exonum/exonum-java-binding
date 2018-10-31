/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.service;

import com.exonum.binding.storage.database.Snapshot;
import java.util.Optional;

/**
 * The current node state on which the blockchain is running.
 * This structure is passed to the `afterCommit` method of the `Service` interface and is used
 * for the interaction between service business logic and the current node state.
 */
public interface BlockCommittedEvent {

  /**
   * If the current node is a validator, returns its identifier.
   * For other nodes return {@code Optional.empty()}.
   */
  Optional<Integer> getValidatorId();

  /**
   * Returns the current blockchain height. This height is "height of the last committed block".
   */
  long getHeight();

  /**
   * Returns the current database snapshot. This snapshot is used to retrieve schema information
   * from the database.
   */
  Snapshot getSnapshot();
}
