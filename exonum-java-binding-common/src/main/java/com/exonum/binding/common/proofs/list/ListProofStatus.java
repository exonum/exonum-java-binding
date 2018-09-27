package com.exonum.binding.common.proofs.list;

import com.exonum.binding.common.proofs.common.ProofStatus;

/**
 * Possible statuses of a checked list proof.
 */
public enum ListProofStatus implements ProofStatus {
  VALID("Proof has a valid structure"),
  INVALID_ELEMENT_NODE_DEPTH("Element node appears below the maximum allowed depth"),
  INVALID_HASH_NODE_DEPTH("HashCode node appears below the maximum allowed depth"),
  INVALID_TREE_NO_ELEMENTS("Tree does not contain any element nodes"),
  INVALID_NODE_DEPTH("Value node appears at the wrong level");

  private final String description;

  ListProofStatus(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return "MapProofStatus{"
        + "description='" + description + '\''
        + '}';
  }
}
