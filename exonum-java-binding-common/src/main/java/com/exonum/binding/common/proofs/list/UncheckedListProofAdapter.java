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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.common.ProofStatus;
import com.exonum.binding.common.serialization.Serializer;

public class UncheckedListProofAdapter<E> implements UncheckedListProof {

  private final ListProofNode listProofNode;

  private final ListProofStructureValidator listProofStructureValidator;

  private final ListProofRootHashCalculator<E> listProofRootHashCalculator;

  public UncheckedListProofAdapter(ListProofNode listProofNode, Serializer<E> serializer) {
    this.listProofNode = checkNotNull(listProofNode);
    this.listProofStructureValidator = new ListProofStructureValidator();
    this.listProofRootHashCalculator = new ListProofRootHashCalculator<>(serializer);
  }

  @Override
  public CheckedListProof check() {
    listProofNode.accept(listProofStructureValidator);
    ProofStatus structureCheckStatus = listProofStructureValidator.check();

    listProofNode.accept(listProofRootHashCalculator);
    HashCode calculatedRootHash = listProofRootHashCalculator.getCalculatedRootHash();

    return new CheckedListProofImpl<>(
        calculatedRootHash, listProofRootHashCalculator.getElements(), structureCheckStatus);
  }
}