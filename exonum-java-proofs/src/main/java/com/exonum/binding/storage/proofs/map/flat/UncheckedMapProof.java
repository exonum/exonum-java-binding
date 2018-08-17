package com.exonum.binding.storage.proofs.map.flat;

import java.util.List;

/**
 * An unchecked map proof.
 * It's used to get a checked map proof.
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
