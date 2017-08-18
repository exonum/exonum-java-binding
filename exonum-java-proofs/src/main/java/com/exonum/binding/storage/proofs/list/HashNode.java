package com.exonum.binding.storage.proofs.list;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a hash of a Merkle sub-tree: a leaf node in proof trees.
 */
public class HashNode implements ListProof {

  private final byte[] hash;

  /**
   * Creates a new hash node.
   */
  public HashNode(byte[] hash) {
    this.hash = checkNotNull(hash);
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns the hash value.
   */
  public byte[] getHash() {
    return hash;
  }
}
