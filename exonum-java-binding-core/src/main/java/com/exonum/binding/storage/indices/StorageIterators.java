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

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;

final class StorageIterators {

  /**
   * Creates a new iterator over an index.
   *
   * <p>The returned iterator is a {@link ConfigurableRustIter}
   * wrapped in a {@link RustIterAdapter}.
   *
   * @param nativeHandle nativeHandle of this iterator
   * @param nextFunction a function to call to get the next item
   * @param disposeOperation an operation to call to destroy the corresponding native iterator
   * @param collectionView a database view of the collection over which to iterate
   * @param modificationCounter a view modification counter
   * @param transformingFunction a function to apply to elements returned by native iterator
   *                             (usually, to an array of bytes)
   */
  static <ElementT, NativeT> Iterator<ElementT> createIterator(
      long nativeHandle,
      LongFunction<NativeT> nextFunction,
      LongConsumer disposeOperation,
      View collectionView,
      ViewModificationCounter modificationCounter,
      Function<? super NativeT, ? extends ElementT> transformingFunction) {

    // Register the destructor first.
    NativeHandle handle = new NativeHandle(nativeHandle);
    Cleaner cleaner = collectionView.getCleaner();
    cleaner.add(new ProxyDestructor(handle, RustIter.class, disposeOperation));

    Iterator<NativeT> iterator = new RustIterAdapter<>(
        new ConfigurableRustIter<>(
            handle,
            nextFunction,
            collectionView,
            modificationCounter
        )
    );

    return Iterators.transform(iterator, transformingFunction::apply);
  }

  private StorageIterators() {}
}
