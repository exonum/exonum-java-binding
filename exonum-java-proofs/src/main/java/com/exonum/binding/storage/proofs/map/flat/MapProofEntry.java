package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;

/**
 * A flat map proof entry corresponding to either a branch node or a leaf node with computed hash
 * in the map tree.
 */
public class MapProofEntry {

  private final DbKey dbKey;

  private final HashCode hash;

  /**
   * Creates a new entry in a flat map proof corresponding to a branch node.
   * @param dbKey a 34-byte database key of the corresponding branch node
   * @param branchNodeHash a hash of the corresponding branch node
   */
  @SuppressWarnings("unused") // Native API
  MapProofEntry(byte[] dbKey, byte[] branchNodeHash) {
    this(DbKey.fromBytes(dbKey), HashCode.fromBytes(branchNodeHash));
  }

  MapProofEntry(DbKey dbKey, HashCode branchNodeHash) {
    this.dbKey = dbKey;
    this.hash = branchNodeHash;
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
