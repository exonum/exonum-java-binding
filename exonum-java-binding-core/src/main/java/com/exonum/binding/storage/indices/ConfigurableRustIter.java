package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A fail-fast iterator.
 *
 * @param <E> type of elements returned by the iterator.
 */
class ConfigurableRustIter<E> extends AbstractNativeProxy implements RustIter<E> {

  private final Function<Long, E> nextFunction;
  private final Consumer<Long> disposeOperation;
  private final View collectionView;
  private final ViewModificationCounter modificationCounter;
  private final Integer initialModCount;

  /**
   * Creates a new iterator over a collection (index).
   *
   * @param nativeHandle nativeHandle of this iterator
   * @param nextFunction a function to call to get the next item
   * @param disposeOperation an operation to call to destroy the corresponding native iterator
   * @param collection a collection over which to iterate
   * @param modificationCounter a view modification counter
   */
  ConfigurableRustIter(long nativeHandle,
                       Function<Long, E> nextFunction,
                       Consumer<Long> disposeOperation,
                       AbstractIndexProxy collection,
                       ViewModificationCounter modificationCounter) {
    super(nativeHandle, true, collection);
    this.nextFunction = nextFunction;
    this.disposeOperation = disposeOperation;
    this.collectionView = collection.dbView;
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

  @Override
  protected void disposeInternal() {
    disposeOperation.accept(getNativeHandle());
  }
}
