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

package com.exonum.binding.common.proofs.common;

import com.exonum.binding.common.hash.HashCode;

/**
 * Common interface for CheckedProof operations.
 */
public interface CheckedProof {

  /**
   * Returns a status of proof verification.
   */
  ProofStatus getProofStatus();

  /**
   * Returns the calculated root hash of the proof.
   * Must be equal to the Merkle root hash of the collection, providing this proof.
   * @throws IllegalStateException if the proof is not valid
   */
  HashCode getRootHash();

  /**
   * Returns true if List Proof status is VALID {@link ProofStatus}, false otherwise.
   */
  boolean isValid();
}
