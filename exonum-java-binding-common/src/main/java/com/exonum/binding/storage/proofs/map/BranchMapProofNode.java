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

package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class BranchMapProofNode implements MapProofNode {

  private final DbKey leftKey;

  private final DbKey rightKey;

  /**
   * Create a new branch node.
   *
   * @param leftKey database key of the left child
   * @param rightKey database key of the right child
   */
  BranchMapProofNode(DbKey leftKey, DbKey rightKey) {
    this.leftKey = checkNotNull(leftKey);
    this.rightKey = checkNotNull(rightKey);
  }

  /**
   * Returns the database key of the left child.
   */
  public DbKey getLeftKey() {
    return leftKey;
  }

  /**
   * Returns the database key of the right child.
   */
  public DbKey getRightKey() {
    return rightKey;
  }
}
