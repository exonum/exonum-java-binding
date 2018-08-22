package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;

/**
 * TODO: note that this is not necessarily a branch but could also be a sibling of requested key
 * A flat map proof entry corresponding to a branch node in the map tree.
 */
public class MapProofEntryBranch extends MapProofEntry {

  private final HashCode hash;

  /**
   * Creates a new entry in a flat map proof corresponding to a branch node.
   * @param dbKey a 34-byte database key of the corresponding branch node
   * @param branchNodeHash a hash of the corresponding branch node
   */
  @SuppressWarnings("unused") // Native API
  MapProofEntryBranch(byte[] dbKey, byte[] branchNodeHash) {
    this(DbKey.fromBytes(dbKey), HashCode.fromBytes(branchNodeHash));
  }

  MapProofEntryBranch(DbKey dbKey, HashCode branchNodeHash) {
    super(dbKey);
    checkArgument(dbKey.getNodeType() == Type.BRANCH, "dbKey should be of type BRANCH", dbKey);
    this.hash = branchNodeHash;
  }

  /**
   * Returns a hash of the keys and values of child nodes.
   */
  @Override
  public HashCode getHash() {
    return hash;
  }
}
