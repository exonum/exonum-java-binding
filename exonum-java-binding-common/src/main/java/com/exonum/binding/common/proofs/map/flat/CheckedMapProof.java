/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.proofs.map.flat;

import com.exonum.binding.common.hash.HashCode;
import java.util.List;

/**
 * A checked map proof.
 * In case of incorrect proof all methods (except for getStatus and compareWithRootHash)
 * throw IllegalStateException.
 * Example usage:
 * <pre><code>
 * byte[] key = "The key for which I want a proved value".getBytes();
 * HashCode expectedRootHash = // get a known root hash from block proof //
 * UncheckedMapProof proof = requestProofForKey(key);
 * // Convert to checked
 * CheckedMapProof checkedProof = proof.check();
 * // Check the root hash
 * if (checkedProof.compareWithRootHash(expectedRootHash)) {
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
