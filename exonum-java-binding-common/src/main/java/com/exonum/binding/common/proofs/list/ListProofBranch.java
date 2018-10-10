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

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents a branch node of a {@link ListProof}.
 *
 * <p>A branch node always has a left child, but there might not be a right child
 * (if the underlying Merkle tree is not a full binary tree).
 */
public final class ListProofBranch implements ListProof {

  private final ListProof left;

  @Nullable
  private final ListProof right;

  public ListProofBranch(ListProof left, @Nullable ListProof right) {
    this.left = checkNotNull(left);
    this.right = right;
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns the left child in the proof tree.
   */
  public ListProof getLeft() {
    return left;
  }

  /**
   * Returns the right child in the proof tree.
   *
   * <p>There might not be a right child if the Merkle tree of the ProofList is not
   * a full binary tree.
   */
  public Optional<ListProof> getRight() {
    return Optional.ofNullable(right);
  }

}
