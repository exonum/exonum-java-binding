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

package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;

/**
 * A proof node for a map that does not contain a mapping for the requested key.
 */
public final class NonEqualValueAtRoot implements MapProof {

  private final DbKey databaseKey;

  private final HashCode valueHash;

  @SuppressWarnings("unused") // native API
  NonEqualValueAtRoot(byte[] databaseKey, byte[] valueHash) {
    this(DbKey.fromBytes(databaseKey), HashCode.fromBytes(valueHash));
  }

  /**
   * Create a new proof node.
   *
   * @param databaseKey a database key
   * @param valueHash a hash of the value mapped to the key
   */
  public NonEqualValueAtRoot(DbKey databaseKey, HashCode valueHash) {
    this.databaseKey = checkNotNull(databaseKey);
    this.valueHash = checkNotNull(valueHash);
  }

  public DbKey getDatabaseKey() {
    return databaseKey;
  }

  public HashCode getValueHash() {
    return valueHash;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
