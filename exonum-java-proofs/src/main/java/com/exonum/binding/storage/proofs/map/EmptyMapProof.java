package com.exonum.binding.storage.proofs.map;

/**
 * A proof node for an empty map.
 */
public final class EmptyMapProof implements MapProof {

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
