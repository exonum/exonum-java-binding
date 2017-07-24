package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A fail-fast iterator.
 *
 * @param <E> type of elements returned by the iterator.
 */
class ConfigurableRustIter<E> extends RustIter<E> {

  private final Function<Long, E> nextFunction;
  private final Consumer<Long> disposeOperation;
  private final View parentView;
  private final ViewModificationCounter modificationCounter;
  private final Integer initialModCount;

  /**
   * Creates a new iterator over a collection (index).
   *
   * @param nativeHandle nativeHandle of this iterator.
   * @param nextFunction a function to call to get the next item.
   * @param disposeOperation an operation to call to destroy the corresponding native iterator.
   * @param parentView a view from which the collection has been created.
   * @param modificationCounter a view modification counter.
   */
  ConfigurableRustIter(long nativeHandle,
                       Function<Long, E> nextFunction,
                       Consumer<Long> disposeOperation,
                       View parentView,
                       ViewModificationCounter modificationCounter) {
    super(nativeHandle, true);
    this.nextFunction = nextFunction;
    this.disposeOperation = disposeOperation;
    this.parentView = parentView;
    this.modificationCounter = modificationCounter;
    this.initialModCount = modificationCounter.getModificationCount(parentView);
  }

  @Override
  public Optional<E> next() {
    checkNotModified();
    return Optional.ofNullable(nextFunction.apply(getNativeHandle()));
  }

  private void checkNotModified() {
    if (modificationCounter.isModifiedSince(parentView, initialModCount)) {
      throw new ConcurrentModificationException("Fork was modified during iteration: "
          + parentView);
    }
  }

  @Override
  void disposeInternal() {
    checkValid(parentView);
    disposeOperation.accept(getNativeHandle());
  }
}
