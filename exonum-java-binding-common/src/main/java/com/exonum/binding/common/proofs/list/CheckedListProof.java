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

package com.exonum.binding.common.proofs.list;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.common.ProofStatus;
import com.exonum.binding.common.proofs.map.flat.MapEntry;
import com.exonum.binding.common.proofs.map.flat.MapProofStatus;
import java.util.List;
import java.util.NavigableMap;

/**
 * A checked list proof.
 * In case of incorrect proof all methods (except for getStatus and compareWithRootHash)
 * throw IllegalStateException.
 * Example usage:
 * <pre><code>
 * byte[] key = "The key for which I want a proved value".getBytes();
 * HashCode expectedRootHash = // get a known root hash from block proof //
 * UncheckedListProof proof = requestProofForKey(key);
 * // Convert to checked
 * CheckedListProof checkedProof = proof.check();
 * // Check the root hash
 * if (checkedProof.getRootHash().equals(expectedRootHash)) {
 *   // Get and use the value(s)
 *   NavigableMap value = checked.getElements();
 * }
 * </code></pre>
 */
public interface CheckedListProof {
  /**
   * Get all leaf entries of this proof.
   * @throws IllegalStateException if the proof is not valid
   */
  NavigableMap getElements();

  /**
   * Return a hash of a proof root node.
   * @throws IllegalStateException if the proof is not valid
   */
  HashCode getRootHash();

  /**
   * Returns the status of this proof: whether it is structurally valid.
   */
  ProofStatus getStatus();

}
