package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.hash.HashCode;


/**
 * A map proof node for a map that does not contain mapping for the requested key.
 */
public class MappingNotFoundProofBranch extends BranchMapProofNode {

  private final HashCode leftHash;

  private final HashCode rightHash;

  /**
   * Create a new branch node.
   *
   * @param leftHash hash of the left sub-tree of the Merkle-Patricia tree
   * @param rightHash hash of the right sub-tree of the Merkle-Patricia tree
   * @param leftKey database key of the left child
   * @param rightKey database key of the right child
   */
  MappingNotFoundProofBranch(HashCode leftHash, HashCode rightHash, DbKey leftKey, DbKey rightKey) {
    super(leftKey, rightKey);
    this.leftHash = checkNotNull(leftHash);
    this.rightHash = checkNotNull(rightHash);
  }

  public HashCode getLeftHash() {
    return leftHash;
  }

  public HashCode getRightHash() {
    return rightHash;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
