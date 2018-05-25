package com.exonum.binding.storage.proofs.list;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;

/**
 * Represents a hash of a Merkle sub-tree: a leaf node in proof trees.
 */
public final class HashNode implements ListProof {

  private final HashCode hash;

  /**
   * Creates a new hash node.
   */
  @SuppressWarnings("unused")  // native API
  HashNode(byte[] hash) {
    this(HashCode.fromBytes(hash));
  }

  public HashNode(HashCode hash) {
    this.hash = checkNotNull(hash);
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns the hash value.
   */
  public HashCode getHash() {
    return hash;
  }
}
