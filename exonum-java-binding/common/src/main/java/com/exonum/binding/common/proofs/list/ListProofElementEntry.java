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

import com.google.auto.value.AutoValue;

/** A value stored in the Merkle tree at its bottom level (at height 0). */
@AutoValue
abstract class ListProofElementEntry implements ListProofEntry {

  /** Returns a value of the element stored at this index in the list. */
  abstract byte[] getElement();

  static ListProofElementEntry newInstance(long index, byte[] element) {
    ListProofEntry.checkIndex(index);
    return new AutoValue_ListProofElementEntry(index, element);
  }
}
