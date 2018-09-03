package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import java.util.List;

/**
 * A checked map proof.
 * In case of incorrect proof all methods (except for getStatus and compareWithRootHash)
 * throw IllegalStateException.
 * Example usage:
 * <pre><code>
 * byte[] key = "The key for which I want a proved value";
 * HashCode expectedRootHash = // get a known root hash from block proof //
 * UncheckedMapProof proof = requestProofForKey(key);
 * // Convert to checked
 * CheckedMapProof checkedProof = proof.check();
 * // Check the root hash
 * if (compareWithRootHash(HashCode expectedRootHash)) {
 *   // Get and use the value(s)
 *   byte[] value = checked.get(key);
 * }
 * </code></pre>
 */
public interface CheckedMapProof {
  /**
   * Get all leaf entries of this proof.
   * @throws IllegalStateException if the proof is not valid
   */
  List<MapEntry> getEntries();

  /**
   * Get all keys that were requested, but did not appear in this proof.
   * @throws IllegalStateException if the proof is not valid
   */
  List<byte[]> getMissingKeys();

  /**
   * If this proof is valid, returns true if there is a given key in the proof;
   * false — if there is no such key.
   * @throws IllegalStateException if the proof is not valid
   */
  boolean containsKey(byte[] key);

  /**
   * Return a hash of a proof root node.
   * @throws IllegalStateException if the proof is not valid
   */
  HashCode getRootHash();

  /**
   * If this proof is valid, returns the value corresponding to the specified key
   * or null if there is no such key in the proof.
   * @throws IllegalStateException if the proof is not valid
   */
  byte[] get(byte[] key);

  /**
   * Returns the status of this proof: whether it is structurally valid.
   */
  ProofStatus getStatus();

  /**
   * Checks that proof is correct and {@code expectedRootHash} is equal to the root hash.
   * @throws IllegalStateException if the proof is not valid
   */
  boolean compareWithRootHash(HashCode expectedRootHash);
}
