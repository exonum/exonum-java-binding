package com.exonum.binding.storage.proofs.map;

/**
 * A proof that a map contains a mapping for some key.
 */
public interface MapProof {
  void accept(MapProofVisitor visitor);
}
