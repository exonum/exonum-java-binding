package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;

/**
 * A flat map proof entry corresponding to a leaf node with a requested key which is absent in the
 * map tree.
 */
public class MapProofAbsentEntryLeaf {

  private final DbKey dbKey;

  /**
   * Creates a new entry in a flat map proof corresponding to a leaf node.
   * @param dbKey a 34-byte database key
   */
  @SuppressWarnings("unused") // Native API
  MapProofAbsentEntryLeaf(byte[] dbKey) {
    this(DbKey.fromBytes(dbKey));
  }

  MapProofAbsentEntryLeaf(DbKey dbKey) {
    checkArgument(dbKey.getNodeType() == Type.LEAF, "dbKey should be of type LEAF", dbKey);
    this.dbKey = dbKey;
  }

  /**
   * Returns a database key of this node.
   */
  public DbKey getDbKey() {
    return dbKey;
  }
}
