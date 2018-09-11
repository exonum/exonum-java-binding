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

package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;

/**
 * A flat map proof entry corresponding to either a branch node or a leaf node with computed hash
 * in a map tree.
 */
public class MapProofEntry {

  private final DbKey dbKey;

  private final HashCode hash;

  /**
   * Creates a new entry in a flat map proof corresponding to a branch or a leaf node.
   * @param dbKey a 34-byte database key of the corresponding branch node
   * @param nodeHash a hash of the corresponding node
   */
  @SuppressWarnings("unused") // Native API
  MapProofEntry(byte[] dbKey, byte[] nodeHash) {
    this(DbKey.fromBytes(dbKey), HashCode.fromBytes(nodeHash));
  }

  MapProofEntry(DbKey dbKey, HashCode nodeHash) {
    this.dbKey = dbKey;
    this.hash = nodeHash;
  }

  /**
   * Returns a database key of this node.
   */
  public DbKey getDbKey() {
    return dbKey;
  }

  /**
   * Returns a hash of the corresponding proof map tree node.
   */
  public HashCode getHash() {
    return hash;
  }
}
