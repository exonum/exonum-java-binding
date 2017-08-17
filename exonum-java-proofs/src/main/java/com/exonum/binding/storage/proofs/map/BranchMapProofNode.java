package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class BranchMapProofNode implements MapProofNode {

  private final DbKey leftKey;

  private final DbKey rightKey;

  /**
   * Create a new branch node.
   *
   * @param leftKey database key of the left child
   * @param rightKey database key of the right child
   */
  BranchMapProofNode(DbKey leftKey, DbKey rightKey) {
    this.leftKey = checkNotNull(leftKey);
    this.rightKey = checkNotNull(rightKey);
  }

  /**
   * Returns the database key of the left child.
   */
  public DbKey getLeftKey() {
    return leftKey;
  }

  /**
   * Returns the database key of the right child.
   */
  public DbKey getRightKey() {
    return rightKey;
  }
}
