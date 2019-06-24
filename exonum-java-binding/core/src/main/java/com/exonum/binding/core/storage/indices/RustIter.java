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

package com.exonum.binding.core.storage.indices;

import java.util.Optional;

/**
 * An interface corresponding to
 * <a href="https://doc.rust-lang.org/std/iter/trait.Iterator.html">std::iter::Iterator</a>
 * from the Rust standard library.
 *
 * @param <E> type of elements returned by this iterator
 */
interface RustIter<E> {

  /**
   * Advance the iterator to the next item.
   *
   * @return the next item or {@link Optional#empty} if the end is reached.
   */
  Optional<E> next();
}
