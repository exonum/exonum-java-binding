package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
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
  static <ElementT, NativeT> StorageIterator<ElementT> createIterator(
      long nativeHandle,
      LongFunction<NativeT> nextFunction,
      LongConsumer disposeOperation,
      View collectionView,
      ViewModificationCounter modificationCounter,
      Function<? super NativeT, ? extends ElementT> transformingFunction) {
    // todo: Use the Guava-provided iterator when ECR-595 is resolved (Iterators#transform)
    RustIterAdapter<NativeT> iterator = new RustIterAdapter<>(
        new ConfigurableRustIter<>(
            nativeHandle,
            nextFunction,
            disposeOperation,
            collectionView,
            modificationCounter
        )
    );
    return transform(iterator, transformingFunction);
  }

  private static <InT, OutT> StorageIterator<OutT> transform(
      StorageIterator<? extends InT> backingIterator,
      Function<? super InT, ? extends OutT> transformingFunction) {
    return new TransformedIterator<>(backingIterator, transformingFunction);
  }

  private StorageIterators() {}
}
