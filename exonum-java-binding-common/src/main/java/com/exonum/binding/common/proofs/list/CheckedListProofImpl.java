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
import java.util.NavigableMap;

/**
 * A checked list proof includes list proof verification results.
 *
 * If it is correct {@link #isValid()} you may access:
 *
 * a calculated Merkle root hash
 * proof elements.
 * If the proof is not valid, you may get the verification status using getStatus {@link #getProofStatus()}
 */
public class CheckedListProofImpl<E> implements CheckedListProof {

  private final HashCode calculatedRootHash;

  private final NavigableMap<Long, E> elements;

  private final ListProofStatus proofStatus;

  /**
   * Creates checked list proof.
   * @param calculatedRootHash calculated root hash of the proof
   * @param elements proof elements collection
   * @param proofStatus a status of proof verification
   */
  public CheckedListProofImpl(HashCode calculatedRootHash,
      NavigableMap<Long, E> elements, ListProofStatus proofStatus) {
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
  public ListProofStatus getProofStatus() {
    return proofStatus;
  }

  @Override
  public boolean isValid() {
    return proofStatus == ListProofStatus.VALID;
  }

  private void checkValid() {
    checkState(isValid(), "Proof is not valid: %s", proofStatus);
  }
}
