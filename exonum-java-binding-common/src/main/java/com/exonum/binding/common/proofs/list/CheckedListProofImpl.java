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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.common.ProofStatus;
import java.util.NavigableMap;

/**
 * Checked List Proof which includes list proof verification results.
 */
public class CheckedListProofImpl<E> implements CheckedListProof {

  private final HashCode calculatedRootHash;

  private final NavigableMap<Long, E> elements;

  private final ProofStatus proofStatus;

  /**
   * Creates checked list proof.
   * @param calculatedRootHash calculated root hash of the proof
   * @param elements proof elements collection
   * @param proofStatus a status of proof verification
   */
  CheckedListProofImpl(HashCode calculatedRootHash,
      NavigableMap<Long, E> elements, ProofStatus proofStatus) {
    this.calculatedRootHash = checkNotNull(calculatedRootHash);
    this.elements = checkNotNull(elements);
    this.proofStatus = checkNotNull(proofStatus);
  }

  @Override
  public NavigableMap<Long, E>  getElements() {
    checkValid();
    return elements;
  }

  @Override
  public HashCode getRootHash() {
    checkValid();
    return calculatedRootHash;
  }

  @Override
  public ProofStatus getStatus() {
    return proofStatus;
  }

  private void checkValid() {
    checkState(proofStatus == ListProofStatus.VALID, "Proof is not valid: %s", proofStatus);
  }
}
