package com.exonum.binding.proxy;

import java.util.Optional;

public abstract class RustIter<E> extends AbstractNativeProxy {
  /**
   * @param nativeHandle a native handle: an implementation-specific reference to a native iterator.
   * @param owningHandle true if this proxy is responsible to release any native resources;
   */
  RustIter(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }

  /**
   * Advance the iterator to the next item.
   *
   * @return the next item or {@link Optional#empty} if the end is reached.
   */
  public abstract Optional<E> next();
}
