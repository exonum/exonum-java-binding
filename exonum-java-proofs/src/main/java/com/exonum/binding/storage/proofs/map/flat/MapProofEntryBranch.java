package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;

/**
 * A flat map proof entry corresponding to a branch node in the map tree.
 */
public class MapProofEntryBranch extends MapProofEntry {

  private final HashCode hash;

  MapProofEntryBranch(DbKey dbKey, HashCode hash) {
    super(dbKey);
    checkArgument(dbKey.getNodeType() == Type.BRANCH, "dbKey should be of type BRANCH", dbKey);
    this.hash = hash;
  }

  /**
   * Returns a hash of the keys and values of child nodes.
   */
  @Override
  public HashCode getHash() {
    return hash;
  }
}
