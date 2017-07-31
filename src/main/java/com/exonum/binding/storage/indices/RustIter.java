package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.NativeProxy;
import java.util.Optional;

/**
 * An interface corresponding to
 * <a href="https://doc.rust-lang.org/std/iter/trait.Iterator.html">std::iter::Iterator</a>
 * from the Rust standard library.
 *
 * @param <E> type of elements returned by this iterator
 */
interface RustIter<E> extends NativeProxy {

  /**
   * Advance the iterator to the next item.
   *
   * @return the next item or {@link Optional#empty} if the end is reached.
   */
  Optional<E> next();
}
