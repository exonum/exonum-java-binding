/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;


/**
 * A map proof node for a map that does not contain mapping for the requested key.
 */
public final class MappingNotFoundProofBranch extends BranchMapProofNode {

  private final HashCode leftHash;

  private final HashCode rightHash;

  @SuppressWarnings("unused")  // native API
  MappingNotFoundProofBranch(byte[] leftHash, byte[] rightHash, byte[] leftKey, byte[] rightKey) {
    this(HashCode.fromBytes(leftHash), HashCode.fromBytes(rightHash),
        DbKey.fromBytes(leftKey), DbKey.fromBytes(rightKey));
  }

  /**
   * Create a new branch node.
   *
   * @param leftHash hash of the left sub-tree of the Merkle-Patricia tree
   * @param rightHash hash of the right sub-tree of the Merkle-Patricia tree
   * @param leftKey database key of the left child
   * @param rightKey database key of the right child
   */
  MappingNotFoundProofBranch(HashCode leftHash, HashCode rightHash, DbKey leftKey, DbKey rightKey) {
    super(leftKey, rightKey);
    this.leftHash = checkNotNull(leftHash);
    this.rightHash = checkNotNull(rightHash);
  }

  public HashCode getLeftHash() {
    return leftHash;
  }

  public HashCode getRightHash() {
    return rightHash;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
