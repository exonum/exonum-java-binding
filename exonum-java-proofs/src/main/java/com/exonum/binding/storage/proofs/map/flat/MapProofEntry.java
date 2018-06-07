package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;

/**
 * An entry in a flat map proof corresponding to a node in the map tree.
 */
public abstract class MapProofEntry {

  private final DbKey dbKey;

  MapProofEntry(DbKey dbKey) {
    this.dbKey = dbKey;
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
  public abstract HashCode getHash();
}
