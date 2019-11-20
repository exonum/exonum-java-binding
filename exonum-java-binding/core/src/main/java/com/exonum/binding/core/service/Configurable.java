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

package com.exonum.binding.core.service;

import com.exonum.binding.core.storage.database.Fork;

/**
 * A configurable Exonum service. Allows services to update their configuration through
 * the supervisor service during their operation.
 *
 * <p>Services may use the same configuration parameters as
 * in {@link Service#initialize(Fork, Configuration)}, or different.
 * TODO: Link the appropriate documentation section on updating the service configuration
 *   through the supervisor
 */
public interface Configurable {

  /**
   * Verifies the correctness of the proposed configuration.
   *
   * <p>This method is called when a new configuration is proposed. If the proposed
   * configuration is correct, this method shall return with no changes to the service data.
   * If it is not valid, this method shall throw an exception.
   *
   * @param fork a view representing the current database state
   * @param configuration a proposed configuration
   * @throws IllegalArgumentException if the proposed configuration is not valid
   */
  void verifyConfiguration(Fork fork, Configuration configuration);

  /**
   * Applies the given configuration to this service. The configuration is guaranteed to be
   * valid according to {@link #verifyConfiguration(Fork, Configuration)}.
   *
   * <p>The implementation shall make any changes to the service persistent state to apply
   * the new configuration, because the supervisor does <em>not</em> store them for later retrieval.
   *
   * @param fork a fork to which to apply changes
   * @param configuration a new valid configuration
   */
  void applyConfiguration(Fork fork, Configuration configuration);
}
