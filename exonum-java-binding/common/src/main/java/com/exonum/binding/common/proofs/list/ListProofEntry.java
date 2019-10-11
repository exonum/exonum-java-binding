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

import static com.google.common.base.Preconditions.checkArgument;

interface ListProofEntry {

  /**
   * The maximum height of a list proof tree.
   */
  int MAX_HEIGHT = 56;

  /**
   * The maximum index of a list proof node: 2^56 - 1.
   */
  long MAX_INDEX = 0xFF_FFFF_FFFF_FFFFL;

  /**
   * Returns the index of the proof tree node at the height of its level. Indexes start
   * from 0 for the leftmost node and up to <em>2^d - 1</em> for the rightmost node,
   * where <em>d = ceil(log2(N)) - h</em> is the depth of the node at height <em>h</em>;
   * <em>N</em> is the number of elements in the tree.
   */
  long getIndex();

  static void checkIndex(long index) {
    checkArgument(0 <= index && index <= MAX_INDEX,
        "Entry index (%s) is out of range [0; 2^56]", index);
  }

  static void checkHeight(int height) {
    checkArgument(0 <= height && height <= MAX_HEIGHT,
        "Entry height (%s) is out of range [0; 56]", height);
  }
}
