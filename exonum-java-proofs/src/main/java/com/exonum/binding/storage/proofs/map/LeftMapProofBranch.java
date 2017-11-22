package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.hash.HashCode;


/**
 * A proof node for a map that might contain mapping for the requested key in the left sub tree.
 */
public class LeftMapProofBranch extends BranchMapProofNode {

  private final MapProofNode left;

  private final HashCode rightHash;

  @SuppressWarnings("unused") // native API
  LeftMapProofBranch(MapProofNode left, byte[] rightHash, byte[] leftKey, byte[] rightKey) {
    this(left, HashCode.fromBytes(rightHash), DbKey.fromBytes(leftKey), DbKey.fromBytes(rightKey));
  }

  /**
   * Create a new branch node with the mapping in the left sub-tree.
   *
   * @param left left proof node
   * @param rightHash hash of the right sub-tree of the Merkle-Patricia tree
   * @param leftKey  database key of the left child
   * @param rightKey database key of the right child
   */
  public LeftMapProofBranch(MapProofNode left, HashCode rightHash, DbKey leftKey, DbKey rightKey) {
    super(leftKey, rightKey);
    this.left = checkNotNull(left);
    this.rightHash = checkNotNull(rightHash);
  }

  public MapProofNode getLeft() {
    return left;
  }

  public HashCode getRightHash() {
    return rightHash;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
