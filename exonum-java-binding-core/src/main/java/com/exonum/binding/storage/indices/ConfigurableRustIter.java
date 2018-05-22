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
class ConfigurableRustIter<E> extends AbstractNativeProxy implements RustIter<E> {

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
