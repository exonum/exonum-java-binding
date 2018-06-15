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

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * A fail-fast iterator.
 *
 * @param <E> type of elements returned by the iterator.
 */
final class ConfigurableRustIter<E> extends AbstractNativeProxy implements RustIter<E> {

  private final LongFunction<E> nextFunction;
  private final View collectionView;
  private final ViewModificationCounter modificationCounter;
  private final Integer initialModCount;

  /**
   * Creates a new iterator over a collection (index).
   *
   * @param nativeHandle nativeHandle of this iterator
   * @param nextFunction a function to call to get the next item
   * @param collectionView a database view of the collection over which to iterate
   * @param modificationCounter a view modification counter
   */
  ConfigurableRustIter(NativeHandle nativeHandle,
                       LongFunction<E> nextFunction,
                       View collectionView,
                       ViewModificationCounter modificationCounter) {
    super(nativeHandle);
    this.nextFunction = nextFunction;
    this.collectionView = collectionView;
    this.modificationCounter = modificationCounter;
    this.initialModCount = modificationCounter.getModificationCount(collectionView);
  }

  @Override
  public Optional<E> next() {
    checkNotModified();
    return Optional.ofNullable(nextFunction.apply(getNativeHandle()));
  }

  private void checkNotModified() {
    if (modificationCounter.isModifiedSince(collectionView, initialModCount)) {
      throw new ConcurrentModificationException("Fork was modified during iteration: "
          + collectionView);
    }
  }
}
