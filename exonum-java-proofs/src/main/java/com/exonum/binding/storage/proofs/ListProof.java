package com.exonum.binding.storage.proofs;

/**
 * Represents a proof that some elements exist in a ProofList at certain positions.
 */
public interface ListProof {

  /**
   * Applies the visitor to this proof node.
   *
   * <p>Most implementations simply call {@code visitor.visit(this);}
   *
   * @param visitor a visitor to apply to this node
   */
  void accept(ListProofVisitor visitor);
}
