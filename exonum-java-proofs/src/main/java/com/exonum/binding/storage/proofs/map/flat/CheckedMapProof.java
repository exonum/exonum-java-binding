package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import java.util.List;

/**
 * A checked map proof.
 */
public interface CheckedMapProof {
  /**
   * Get all leaf entries of this proof.
   */
  List<MapProofEntryLeaf> getEntries();

  /**
   * Checks if a leaf entry contains a key.
   */
  boolean containsKey(byte[] key);

  /**
   * Return a hash of a proof root node.
   */
  HashCode getMerkleRoot();

  /**
   * Get a value, corresponding to specified key.
   */
  byte[] get(byte[] key);

  /**
   * Get status of this proof.
   */
  ProofStatus getStatus();
}
