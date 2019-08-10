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

  private final ListProof listProof;

  private final ListProofStructureValidator listProofStructureValidator;

  private final ListProofHashCalculator<E> listProofHashCalculator;

  /**
   * Creates UncheckedListProofAdapter for convenient usage of ListProof interfaces.
   *
   * <p>UncheckedListProofAdapter {@link #check()} method will return CheckedListProof containing
   * results of list proof verification.
   *
   * @param listProof source list proof with index length
   * @param serializer proof elements serializer
   */
  public UncheckedListProofAdapter(ListProof listProof, Serializer<E> serializer) {
    Preconditions.checkNotNull(listProof, "ListProof node must be not null");
    Preconditions.checkNotNull(serializer, "Serializer must be not null");

    this.listProof = listProof;
    this.listProofStructureValidator = new ListProofStructureValidator(listProof.getRootNode());
    this.listProofHashCalculator = new ListProofHashCalculator<>(listProof, serializer);
  }

  @Override
  public CheckedListProof check() {
    ListProofStatus structureCheckStatus = listProofStructureValidator.getProofStatus();
    HashCode calculatedIndexHash = listProofHashCalculator.getHash();

    return new CheckedListProofImpl<>(
        calculatedIndexHash, listProofHashCalculator.getElements(), structureCheckStatus);
  }

  @Override
  public ListProof getListProof() {
    return listProof;
  }
}
