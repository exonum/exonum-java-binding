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

package com.exonum.binding.core.runtime;

import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.storage.database.AbstractAccess;

/**
 * A factory of {@link BlockchainData}.
 *
 * <p>Enables easier unit testing of the {@link ServiceRuntime} and {@link ServiceNodeProxy}.
 */
interface BlockchainDataFactory {

  /**
   * Creates a BlockchainData for the service with the given name.
   *
   * @see BlockchainData#fromRawAccess(AbstractAccess, String)
   */
  default BlockchainData fromRawAccess(AbstractAccess rawAccess, String serviceName) {
    return BlockchainData.fromRawAccess(rawAccess, serviceName);
  }
}
