package com.exonum.binding.storage.proofs.map.flat;

import java.util.List;

public interface UncheckedMapProof {
  /**
   * Get all entries of this proof.
   */
  List<MapProofEntry> getProofList();

  /**
   * Checks that a proof has either correct or incorrect structure and returns a CheckedMapProof.
   */
  CheckedMapProof check();
}
