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

import com.exonum.binding.common.proofs.common.CheckedProof;
import java.util.NavigableMap;

/**
 * A proof that some elements exist in a proof list.
 * Example usage:
 * <pre><code>
 * byte[] key = "The key for which I want a proved value".getBytes();
 * HashCode expectedRootHash = // get a known root hash from block proof //
 * UncheckedListProof proof = new UncheckedListProofAdapter(proof, serializer);
 * // Check the proof
 * CheckedListProof checkedProof = proof.check();
 * // Check the root hash
 * if (checkedProof.isValid()) {
 *   if (checkedProof.getRootHash().equals(expectedRootHash)) {
 *   // Get and use elements
 *   NavigableMap value = checked.getElements();
 *   }
 * }
 * </code></pre>
 */
public interface CheckedListProof<E> extends CheckedProof {
  /**
   * Get all list elements. There might be several consecutive ranges.
   * @throws IllegalStateException if the proof is not valid
   */
  NavigableMap<Long, E> getElements();

  /**
   * Returns the status of this proof: whether it is structurally valid.
   */
  @Override
  ListProofStatus getProofStatus();
}
