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
import com.exonum.binding.common.serialization.Serializer;
import com.google.common.base.Preconditions;

/**
 * An Adapter class used to simplify work with ListProof interfaces.
 */
public class UncheckedListProofAdapter<E> implements UncheckedListProof {

  private final ListProofStructureValidator listProofStructureValidator;

  private final ListProofRootHashCalculator<E> listProofRootHashCalculator;

  /**
   * Creates UncheckedListProofAdapter for convenient usage of ListProof interfaces.
   *
   * <p>UncheckedListProofAdapter check() method will return CheckedListProof containing results of
   * list proof verification.
   *
   * @param listProofNode source list proof
   * @param serializer proof elements serializer
   */
  public UncheckedListProofAdapter(ListProofNode listProofNode, Serializer<E> serializer) {
    Preconditions.checkNotNull(listProofNode, "ListProof node must be not null");
    Preconditions.checkNotNull(serializer, "Serializer must be not null");

    this.listProofStructureValidator = new ListProofStructureValidator(listProofNode);
    this.listProofRootHashCalculator = new ListProofRootHashCalculator<>(listProofNode, serializer);
  }

  @Override
  public CheckedListProof check() {
    ListProofStatus structureCheckStatus = listProofStructureValidator.getProofStatus();
    HashCode calculatedRootHash = listProofRootHashCalculator.getCalculatedRootHash();

    return new CheckedListProofImpl<>(
        calculatedRootHash, listProofRootHashCalculator.getElements(), structureCheckStatus);
  }
}
