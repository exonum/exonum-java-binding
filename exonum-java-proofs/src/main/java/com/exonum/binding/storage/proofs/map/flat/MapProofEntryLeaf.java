package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;

/**
 * A flat map proof entry corresponding to a leaf node with a requested key in the map tree.
 */
public class MapProofEntryLeaf {

  private final DbKey dbKey;

  private final byte[] value;

  /**
   * Creates a new entry in a flat map proof corresponding to a leaf node.
   * @param dbKey a 34-byte database key
   * @param value a value mapped to the key
   */
  @SuppressWarnings("unused") // Native API
  MapProofEntryLeaf(byte[] dbKey, byte[] value) {
    this(DbKey.fromBytes(dbKey), value);
  }

  MapProofEntryLeaf(DbKey dbKey, byte[] value) {
    checkArgument(dbKey.getNodeType() == Type.LEAF, "dbKey should be of type LEAF", dbKey);
    this.dbKey = dbKey;
    this.value = value;
  }

  /**
   * Returns a database key of this node.
   */
  public DbKey getDbKey() {
    return dbKey;
  }

  /** Returns the value of this entry. */
  public byte[] getValue() {
    return value;
  }
}
