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

/**
 * A proof node for a singleton proof map, that contains mapping for the specified key.
 */
public final class EqualValueAtRoot implements MapProof {

  private final DbKey databaseKey;

  private final byte[] value;

  @SuppressWarnings("unused") // native API
  EqualValueAtRoot(byte[] databaseKey, byte[] value) {
    this(DbKey.fromBytes(databaseKey), value);
  }

  /**
   * Creates a new proof node.
   *
   * @param databaseKey a database key
   * @param value a value mapped to the key
   */
  public EqualValueAtRoot(DbKey databaseKey, byte[] value) {
    this.databaseKey = checkNotNull(databaseKey);
    this.value = checkNotNull(value);
  }

  public DbKey getDatabaseKey() {
    return databaseKey;
  }

  public byte[] getValue() {
    return value;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
