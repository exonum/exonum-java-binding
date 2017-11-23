package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;

/**
 * A proof node for a map that might contain mapping for the requested key in the right sub tree.
 */
public class RightMapProofBranch extends BranchMapProofNode {

  private final HashCode leftHash;

  private final MapProofNode right;

  @SuppressWarnings("unused") // native API
  RightMapProofBranch(byte[] leftHash, MapProofNode right, byte[] leftKey, byte[] rightKey) {
    this(HashCode.fromBytes(leftHash), right, DbKey.fromBytes(leftKey), DbKey.fromBytes(rightKey));
  }

  /**
   * Create a new branch node with the mapping in the right sub-tree.
   *
   * @param leftHash hash of the left sub-tree of the Merkle-Patricia tree
   * @param right right proof node
   * @param leftKey  database key of the left child
   * @param rightKey database key of the right child
   */
  RightMapProofBranch(HashCode leftHash, MapProofNode right, DbKey leftKey, DbKey rightKey) {
    super(leftKey, rightKey);
    this.leftHash = checkNotNull(leftHash);
    this.right = checkNotNull(right);
  }

  public HashCode getLeftHash() {
    return leftHash;
  }

  public MapProofNode getRight() {
    return right;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
