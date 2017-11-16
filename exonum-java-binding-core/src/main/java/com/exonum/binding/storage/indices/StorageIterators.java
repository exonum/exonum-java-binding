package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.database.ViewModificationCounter;
import java.util.function.Consumer;
import java.util.function.Function;

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
   * @param parent a collection over which to iterate
   * @param modificationCounter a view modification counter
   */
  static <E> StorageIterator<E> createIterator(long nativeHandle,
                                               Function<Long, E> nextFunction,
                                               Consumer<Long> disposeOperation,
                                               AbstractIndexProxy parent,
                                               ViewModificationCounter modificationCounter) {
    return new RustIterAdapter<>(
        new ConfigurableRustIter<>(
            nativeHandle,
            nextFunction,
            disposeOperation,
            parent,
            modificationCounter
        )
    );
  }

  private StorageIterators() {}
}
