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
 *
 */

package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.google.common.annotations.VisibleForTesting;

/**
 * Provides read-only access to the blockchain state
 * and informational indexes maintained by Exonum core.
 */
public final class Blockchain {

  private final CoreSchemaProxy schema;

  @VisibleForTesting
  Blockchain(CoreSchemaProxy schema) {
    this.schema = schema;
  }

  /**
   * Constructs a new blockchain instance for the given database view.
   */
  public static Blockchain newInstance(View view) {
    CoreSchemaProxy coreSchema = CoreSchemaProxy.newInstance(view);
    return new Blockchain(coreSchema);
  }

  /**
   * Returns the height of the latest committed block.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  public long getHeight() {
    return schema.getHeight();
  }

  /**
   * Returns an list index containing a block hash for every block height
   * (represented by list index id).
   */
  public ListIndex<HashCode> getAllBlockHashes() {
    return schema.getAllBlockHashes();
  }

  /**
   * Returns an proof list index containing block hashes for the given height.
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(long height) {
    return schema.getBlockTransactions(height);
  }

}
