package com.exonum.binding.storage.proofs.map.flat;

/**
 * Possible statuses of a checked map proof.
 */
public enum ProofStatus {
  CORRECT("Proof has a valid structure"),
  NON_TERMINAL_NODE("Leaf entry is of branch type"),
  INVALID_STRUCTURE("Proof has invalid structure"),
  INVALID_ORDER("Proof entries are placed in the wrong order"),
  DUPLICATE_PATH("Proof entries are placed in the wrong order");

  final String description;

  ProofStatus(String description) {
    this.description = description;
  }
}
