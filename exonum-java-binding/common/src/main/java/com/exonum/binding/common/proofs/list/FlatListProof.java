/*
 * Copyright 2019 The Exonum Team
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

import java.util.List;

/**
 * A flat list proof. It proves that certain elements are present in a proof list
 * of a certain size.
 */
class FlatListProof {
  private final List<ListProofElementEntry> elements;
  private final List<ListProofHashedEntry> proof;
  private final long size;

  FlatListProof(List<ListProofElementEntry> elements,
      List<ListProofHashedEntry> proof, long size) {
    this.elements = checkNotNull(elements);
    this.proof = checkNotNull(proof);
    this.size = size;
  }

  CheckedListProof<byte[]> verify() {
    return null;
  }
}
