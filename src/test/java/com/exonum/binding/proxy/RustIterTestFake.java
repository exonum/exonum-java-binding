package com.exonum.binding.proxy;

import java.util.Iterator;
import java.util.Optional;

/**
 * A simple RustIter fake with no native code.
 *
 * <p>An adapter of {@link Iterator} to {@link RustIter}.
 */
public class RustIterTestFake extends RustIter<Integer> {
  private final Iterator<Integer> iterator;

  public RustIterTestFake(Iterable<Integer> iterable) {
    super(1L, true);
    this.iterator = iterable.iterator();
  }

  @Override
  public Optional<Integer> next() {
    return iterator.hasNext() ? Optional.of(iterator.next())
                              : Optional.empty();
  }

  @Override
  void disposeInternal() {
    // no-op
  }
}
