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

/**
 * Represents a proof that some elements exist in a ProofList at certain positions and the length
 * of corresponding index.
 */
public final class ListProof {

  private final ListProofNode rootNode;

  private final long length;

  public ListProof(ListProofNode rootNode, long length) {
    this.rootNode = checkNotNull(rootNode);
    this.length = length;
  }

  /**
   * Returns the root node of the proof tree.
   */
  public ListProofNode getRootNode() {
    return rootNode;
  }

  /**
   * Returns the length of the corresponding index.
   */
  public long getLength() {
    return length;
  }
}
