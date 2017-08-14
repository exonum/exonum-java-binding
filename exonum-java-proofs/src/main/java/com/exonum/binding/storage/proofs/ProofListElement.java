package com.exonum.binding.storage.proofs;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an element of a proof list: a leaf node in a list proof tree.
 */
public class ProofListElement implements ListProof {

  private final byte[] element;

  /**
   * Creates a new ProofListElement.
   *
   * @param element an element of the list
   * @throws NullPointerException if the element is null
   */
  public ProofListElement(byte[] element) {
    this.element = checkNotNull(element);
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns the value of the element.
   */
  public byte[] getElement() {
    return element;
  }
}
