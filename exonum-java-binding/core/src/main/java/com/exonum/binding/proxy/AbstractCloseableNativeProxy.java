/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A proxy of a native object.
 *
 * <p>A native proxy references the corresponding native object by its
 * implementation-specific handle. If handle is zero, the proxy is considered invalid
 * and will not permit referencing any native object. It is perfectly possible to get
 * an invalid proxy (e.g., if a native method fails to allocate a native object).
 *
 * <p>You must close a native proxy when it is no longer needed
 * to release any resources it holds (e.g., destroy a native object).
 * You may use a <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">try-with-resources</a>
 * statement to do that in orderly fashion.
 * When a proxy is closed, it becomes invalid.
 */
public abstract class AbstractCloseableNativeProxy extends AbstractNativeProxy
    implements CloseableNativeProxy {

  /**
   * Whether this proxy shall dispose any resources when closed
   * (e.g., if it owns the corresponding native object and is responsible to clean it up).
   */
  private final boolean dispose;

  /**
   * Proxies that this one references, including transitive references.
   * Each of these must be valid at each native call.
   */
  private final Set<AbstractCloseableNativeProxy> referenced;

  /**
   * Creates a native proxy.
   *
   * @param nativeHandle an implementation-specific reference to a native object
   * @param dispose true if this proxy is responsible to release any resources
   *                by calling {@link #disposeInternal}; false — otherwise
   */
  protected AbstractCloseableNativeProxy(long nativeHandle, boolean dispose) {
    this(nativeHandle, dispose, Collections.emptySet());
  }

  /**
   * Creates a native proxy.
   *
   * @param nativeHandle an implementation-specific reference to a native object
   * @param dispose true if this proxy is responsible to release any resources
   *                by calling {@link #disposeInternal}; false — otherwise
   * @param referenced a referenced native object, that must be alive
   *                   during the operation of this native proxy
   */
  protected AbstractCloseableNativeProxy(long nativeHandle, boolean dispose,
                                         AbstractCloseableNativeProxy referenced) {
    this(nativeHandle, dispose, singleton(checkNotNull(referenced)));
  }

  /**
   * Creates a native proxy.
   *
   * @param nativeHandle an implementation-specific reference to a native object
   * @param dispose true if this proxy is responsible to release any resources
   *                by calling {@link #disposeInternal}; false — otherwise
   * @param referenced a collection of referenced native objects, that must be alive
   *                   during the operation of this native proxy
   */
  protected AbstractCloseableNativeProxy(long nativeHandle, boolean dispose,
                                         Collection<AbstractCloseableNativeProxy> referenced) {
    super(new NativeHandle(nativeHandle));
    this.dispose = dispose;
    this.referenced = getTransitivelyReferenced(referenced);
  }

  private Set<AbstractCloseableNativeProxy> getTransitivelyReferenced(
      Collection<AbstractCloseableNativeProxy> referenced) {
    // You don't have to perform a recursive flat map because each of the proxies
    // this proxy references has all its transitive dependencies.
    return referenced.stream()
        .flatMap((proxy) -> Stream.concat(Stream.of(proxy), proxy.referenced.stream()))
        .collect(Collectors.toSet());
  }

  /**
   * Returns a native implementation-specific handle if it may be safely used
   * to access the native object.
   *
   * <p>The returned value shall only be passed as an argument to native methods.
   *
   * <p>Warning: do not cache the return value, as you won't be able to catch use-after-free.
   *
   * @throws IllegalStateException if this native proxy or any directly or transitively referenced
   *     proxies are invalid (closed or nullptr).
   */
  @Override
  protected final long getNativeHandle() {
    checkAllRefsValid();
    return super.getNativeHandle();
  }

  @Override
  public final void close() {
    if (isValidHandle()) {
      try {
        checkAllRefsValid();
        if (dispose) {
          disposeInternal();
        }
      } finally {
        invalidate();
      }
    }
  }

  private void checkAllRefsValid() {
    if (!allRefsValid()) {
      throw new IllegalStateException(getInvalidProxyErrMessage());
    }
  }

  private boolean allRefsValid() {
    return isValidHandle() && referenced.stream()
        .allMatch(AbstractCloseableNativeProxy::isValidHandle);
  }

  private String getInvalidProxyErrMessage() {
    if (!isValidHandle()) {
      return String.format("This proxy (%s) is not valid", this);
    }
    Set<AbstractCloseableNativeProxy> invalidReferenced = getInvalidReferences();
    return String.format("This proxy (%s) references some invalid proxies: %s",
        this, invalidReferenced);
  }

  @VisibleForTesting
  Set<AbstractCloseableNativeProxy> getInvalidReferences() {
    return referenced.stream()
        .filter(p -> !p.isValidHandle())
        .collect(Collectors.toSet());
  }

  /**
   * Releases any resources owned by this proxy (e.g., the corresponding native object).
   *
   * <p>This method is only called once from {@link #close()} for a <strong>valid</strong>
   * proxy and shall not be called directly.
   */
  protected abstract void disposeInternal();

  private void invalidate() {
    nativeHandle.close();
  }
}
