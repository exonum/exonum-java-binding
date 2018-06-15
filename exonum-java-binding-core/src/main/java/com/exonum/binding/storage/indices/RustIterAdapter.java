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

package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Adapts {@link RustIter} interface to {@link Iterator} interface.
 *
 * @param <E> type of the entry.
 */
final class RustIterAdapter<E> implements Iterator<E> {

  private final RustIter<E> rustIter;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<E> nextItem;

  RustIterAdapter(RustIter<E> rustIter) {
    this.rustIter = checkNotNull(rustIter);
    this.nextItem = rustIter.next();
  }

  @Override
  public boolean hasNext() {
    return nextItem.isPresent();
  }

  @Override
  public E next() {
    if (!hasNext()) {
      throw new NoSuchElementException("Reached the end of the underlying collection. "
          + "Use #hasNext to check if you have reached the end of the collection.");
    }
    Optional<E> nextElement = nextItem;
    nextItem = rustIter.next();  // an after-the-next item
    return nextElement.get();
  }
}
