package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;

/**
 * A flat map proof entry, which stands for a branch node in the corresponding map tree.
 */
public class MapProofEntryBranch extends MapProofEntry {

  private final HashCode hash;

  MapProofEntryBranch(DbKey dbKey, HashCode hash) {
    super(dbKey);
    checkArgument(dbKey.getNodeType() == Type.BRANCH, "dbKey should be of type BRANCH", dbKey);
    this.hash = hash;
  }

  /**
   * Returns a hash of this branch node.
   * @return HashCode of this node
   */
  @Override
  public HashCode getHash() {
    return hash;
  }
}
