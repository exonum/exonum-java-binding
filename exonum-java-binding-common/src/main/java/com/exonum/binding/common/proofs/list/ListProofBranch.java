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

import static com.exonum.binding.common.proofs.list.ListProofStructureValidator.NodeType;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents a branch node of a {@link ListProofNode}.
 *
 * <p>A branch node always has a left child, but there might not be a right child
 * (if the underlying Merkle tree is not a full binary tree).
 */
public final class ListProofBranch implements ListProofNode {

  private final ListProofNode left;

  private final NodeType nodeType = NodeType.BRANCH;

  @Nullable
  private final ListProofNode right;

  public ListProofBranch(ListProofNode left, @Nullable ListProofNode right) {
    this.left = checkNotNull(left);
    this.right = right;
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public NodeType getNodeType() {
    return nodeType;
  }

  /**
   * Returns the left child in the proof tree.
   */
  public ListProofNode getLeft() {
    return left;
  }

  /**
   * Returns the right child in the proof tree.
   *
   * <p>There might not be a right child if the Merkle tree of the ProofList is not
   * a full binary tree.
   */
  public Optional<ListProofNode> getRight() {
    return Optional.ofNullable(right);
  }


}
