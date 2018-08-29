package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
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
