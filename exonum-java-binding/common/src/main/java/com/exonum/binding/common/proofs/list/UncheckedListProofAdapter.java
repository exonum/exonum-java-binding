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

/**
 * An Adapter class used to simplify work with ListProof interfaces.
 */
// todo: Remove the whole thing (LPNode, ...)?
public class UncheckedListProofAdapter implements UncheckedListProof {

  private final ListProofNode rootNode;

  private final ListProofStructureValidator listProofStructureValidator;

  private final ListProofHashCalculator listProofHashCalculator;

  /**
   * Creates UncheckedListProofAdapter for convenient usage of ListProof interfaces.
   *
   * <p>UncheckedListProofAdapter {@link #check()} method will return CheckedListProof containing
   * results of list proof verification.
   *
   * @param rootNode the root node of the proof tree
   * @param length the length of the corresponding index (needed for index hash calculation)
   */
  public UncheckedListProofAdapter(ListProofNode rootNode, long length) {
    this.rootNode = checkNotNull(rootNode);
    this.listProofStructureValidator = new ListProofStructureValidator(rootNode);
    this.listProofHashCalculator = new ListProofHashCalculator(rootNode, length);
  }

  @Override
  public CheckedListProof check() {
    ListProofStatus structureCheckStatus = listProofStructureValidator.getProofStatus();
    HashCode calculatedIndexHash = listProofHashCalculator.getHash();

    return new CheckedListProofImpl<>(0,
        calculatedIndexHash, listProofHashCalculator.getElements(), structureCheckStatus);
  }

  @Override
  public ListProofNode getListProofRootNode() {
    return rootNode;
  }
}
