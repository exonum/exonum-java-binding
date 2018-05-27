package com.exonum.binding.storage.proofs.list;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents a branch node of a {@link ListProof}.
 *
 * <p>A branch node always has a left child, but there might not be a right child
 * (if the underlying Merkle tree is not a full binary tree).
 */
public final class ListProofBranch implements ListProof {

  private final ListProof left;

  @Nullable
  private final ListProof right;

  public ListProofBranch(ListProof left, @Nullable ListProof right) {
    this.left = checkNotNull(left);
    this.right = right;
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns the left child in the proof tree.
   */
  public ListProof getLeft() {
    return left;
  }

  /**
   * Returns the right child in the proof tree.
   *
   * <p>There might not be a right child if the Merkle tree of the ProofList is not
   * a full binary tree.
   */
  public Optional<ListProof> getRight() {
    return Optional.ofNullable(right);
  }
}
