package com.exonum.binding.common.proofs.list;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a proof that some elements exist in a ProofList at certain positions and the length
 * of corresponding index.
 */
public final class ListProof {

  private final ListProofNode rootNode;

  private final long length;

  public ListProof(ListProofNode rootNode, long length) {
    this.rootNode = checkNotNull(rootNode);
    this.length = length;
  }

  /**
   * Returns the root node of the proof tree.
   */
  public ListProofNode getRootNode() {
    return rootNode;
  }

  /**
   * Returns the length of the corresponding index.
   */
  public long getLength() {
    return length;
  }
}
