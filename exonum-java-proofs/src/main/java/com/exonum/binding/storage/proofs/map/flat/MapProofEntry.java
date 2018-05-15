package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;

/**
 * A flat map proof entry, which stands for a node in the corresponding map tree.
 */
public abstract class MapProofEntry {

  private final DbKey dbKey;

  MapProofEntry(DbKey dbKey) {
    this.dbKey = dbKey;
  }

  /**
   * Returns a key of this node.
   * @return DbKey of this node
   */
  public DbKey getDbKey() {
    return dbKey;
  }

  /**
   * Returns a hash of this node, which is hash of a value in case of a leaf node and a hash, based
   * on children hashes and keys in case of a branch node.
   * @return HashCode of this node
   */
  public abstract HashCode getHash();
}
