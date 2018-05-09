package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;

/**
 * A proof entry for a flat map.
 */
public abstract class MapProofEntry {

  private final DbKey dbKey;

  MapProofEntry(DbKey dbKey) {
    this.dbKey = dbKey;
  }

  public DbKey getDbKey() {
    return dbKey;
  }

  public abstract HashCode getHash();
}
