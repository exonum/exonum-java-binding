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

  private final ListProofRoot listProofRoot;

  private final ListProofStructureValidator listProofStructureValidator;

  private final ListMerkleRootCalculator<E> listMerkleRootCalculator;

  /**
   * Creates UncheckedListProofAdapter for convenient usage of ListProof interfaces.
   *
   * <p>UncheckedListProofAdapter {@link #check()} method will return CheckedListProof containing
   * results of list proof verification.
   *
   * @param listProof source list proof with index length
   * @param serializer proof elements serializer
   */
  public UncheckedListProofAdapter(ListProofRoot listProof, Serializer<E> serializer) {
    Preconditions.checkNotNull(listProof, "ListProof node must be not null");
    Preconditions.checkNotNull(serializer, "Serializer must be not null");

    this.listProofRoot = listProof;
    this.listProofStructureValidator = new ListProofStructureValidator(listProof.getRootNode());
    this.listMerkleRootCalculator = new ListMerkleRootCalculator<>(listProof, serializer);
  }

  @Override
  public CheckedListProof check() {
    ListProofStatus structureCheckStatus = listProofStructureValidator.getProofStatus();
    HashCode calculatedRootHash = listMerkleRootCalculator.getMerkleRoot();

    return new CheckedListProofImpl<>(
        calculatedRootHash, listMerkleRootCalculator.getElements(), structureCheckStatus);
  }

  @Override
  public ListProofRoot getListProofRoot() {
    return listProofRoot;
  }
}
