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

import com.exonum.binding.common.hash.HashCode;
import com.google.auto.value.AutoValue;

/**
 * A hash of a sub-tree in a Merkle proof tree.
 */
@AutoValue
public abstract class ListProofHashedEntry implements ListProofEntry {

  /**
   * Returns the height of the proof tree node corresponding to this entry.
   * The height of leaf nodes is equal to 0; the height of the root, or top node:
   * <em>ceil(log2(N))</em>.
   */
  abstract int getHeight();

  /**
   * Returns the hash of the sub-tree this entry represents.
   */
  abstract HashCode getHash();

  public static ListProofHashedEntry newInstance(long index, int height, HashCode nodeHash) {
    ListProofEntry.checkIndex(index);
    ListProofEntry.checkHeight(height);
    return new AutoValue_ListProofHashedEntry(index, height, nodeHash);
  }
}
