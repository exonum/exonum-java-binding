package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;

/**
 * A flat map proof entry corresponding to a leaf node in the map tree.
 */
public class MapProofEntryLeaf extends MapProofEntry {

  private final byte[] value;

  private final HashFunction hashFunction;

  /**
   * Creates a new entry in a flat map proof corresponding to a leaf node.
   * @param dbKey a 34-byte database key
   * @param value a value mapped to the key
   */
  @SuppressWarnings("unused") // Native API
  MapProofEntryLeaf(byte[] dbKey, byte[] value) {
    this(DbKey.fromBytes(dbKey), value, Hashing.sha256());
  }

  MapProofEntryLeaf(DbKey dbKey, byte[] value, HashFunction hashFunction) {
    super(dbKey);
    checkArgument(dbKey.getNodeType() == Type.LEAF, "dbKey should be of type LEAF", dbKey);
    this.value = value;
    this.hashFunction = hashFunction;
  }

  public byte[] getValue() {
    return value;
  }

  /**
   * Returns a hash of the value.
   */
  @Override
  public HashCode getHash() {
    return hashFunction.hashBytes(value);
  }
}
