/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.indices;

import com.exonum.binding.common.hash.HashCode;

/**
 * A proof entry is a {@linkplain HashableIndex hashable} variant of {@link EntryIndex}.
 *
 * @param <T> the type of an element in this entry
 */
public interface ProofEntryIndex<T> extends EntryIndex<T>, HashableIndex {

  /**
   * Returns the index hash which represents the complete state of this entry.
   * Any modifications to this entry affect the index hash.
   *
   * <p>The entry index hash is computed as SHA-256 of the entry binary representation, or
   * a hash of zeroes if the entry is not set.
   *
   * @throws IllegalStateException if the index is invalid
   */
  @Override
  HashCode getIndexHash();
}
