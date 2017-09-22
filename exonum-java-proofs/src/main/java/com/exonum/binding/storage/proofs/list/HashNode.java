package com.exonum.binding.storage.proofs.list;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.hash.HashCode;

/**
 * Represents a hash of a Merkle sub-tree: a leaf node in proof trees.
 */
public class HashNode implements ListProof {

  private final HashCode hash;

  /**
   * Creates a new hash node.
   */
  public HashNode(byte[] hash) {
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
