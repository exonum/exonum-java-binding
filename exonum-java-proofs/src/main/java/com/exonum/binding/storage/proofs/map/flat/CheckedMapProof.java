package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import java.util.List;

/**
 * A checked map proof.
 * In case of incorrect proof all methods (except for getStatus) throw IllegalStateException.
 */
public interface CheckedMapProof {
  /**
   * Get all leaf entries of this proof.
   * @throws IllegalStateException if the proof is not valid
   */
  List<MapProofEntryLeaf> getEntries();

  /**
   * Checks if a leaf entry contains a key.
   * @throws IllegalStateException if the proof is not valid
   */
  boolean containsKey(byte[] key);

  /**
   * Return a hash of a proof root node.
   * @throws IllegalStateException if the proof is not valid
   */
  HashCode getMerkleRoot();

  /**
   * Get a value, corresponding to specified key.
   * @throws IllegalStateException if the proof is not valid
   */
  byte[] get(byte[] key);

  /**
   * Get status of this proof.
   */
  ProofStatus getStatus();
}
