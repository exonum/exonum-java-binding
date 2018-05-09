package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;

/**
 * A branch proof entry for a flat map.
 */
public class MapProofEntryBranch extends MapProofEntry {

  private final HashCode hash;

  MapProofEntryBranch(DbKey dbKey, HashCode hash) {
    super(dbKey);
    checkArgument(dbKey.getNodeType() == Type.BRANCH, "dbKey should be of type BRANCH", dbKey);
    this.hash = hash;
  }

  @Override
  public HashCode getHash() {
    return hash;
  }
}
