package com.exonum.binding.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.RustIter;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Adapts {@link RustIter} interface to {@link StorageIterator} interface.
 *
 * @param <E> type of the entry.
 */
public class RustIterAdapter<E> implements StorageIterator<E> {

  private final RustIter<E> rustIter;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<E> nextItem;

  public RustIterAdapter(RustIter<E> rustIter) {
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

  @Override
  public void close() {
    rustIter.close();
  }
}
