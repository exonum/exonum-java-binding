/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.proxy;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Optional;
import java.util.function.LongConsumer;

/**
 * ProxyDestructor is a clean action that destroys a native proxy and closes its native handle.
 *
 * <p>Native proxies do not implement any interface (e.g., {@link CloseableNativeProxy})
 * and use this class so that there is no public #close method available in the interface
 * of the proxy, making the risk of misuse smaller.
 *
 * <p>All method parameters are non-null by default.
 *
 * <p>This class is not thread-safe.
 */
public final class ProxyDestructor implements CleanAction<Class<?>> {

  private final NativeHandle nativeHandle;
  private final LongConsumer cleanFunction;
  private final Class<?> proxyClass;
  private boolean destroyed;

  /**
   * Creates a new destructor of a native proxy, registered in the given cleaner.
   *
   * @param cleaner a cleaner to register the destructor in
   * @param nativeHandle a handle to the native object
   * @param proxyClass a class of proxy
   * @param destructorFunction a clean function to perform
   */
  @CanIgnoreReturnValue
  public static ProxyDestructor newRegistered(Cleaner cleaner,
                                              NativeHandle nativeHandle,
                                              Class<?> proxyClass,
                                              LongConsumer destructorFunction) {
    ProxyDestructor d = new ProxyDestructor(nativeHandle, proxyClass, destructorFunction);
    cleaner.add(d);
    return d;
  }

  /**
   * Creates a new destructor of a native proxy.
   *
   * @param nativeHandle a handle to the native object
   * @param proxyClass a class of the proxy
   * @param destructorFunction a clean function to perform
   */
  public ProxyDestructor(NativeHandle nativeHandle, Class<?> proxyClass,
                         LongConsumer destructorFunction) {
    this.nativeHandle = checkNotNull(nativeHandle);
    this.cleanFunction = checkNotNull(destructorFunction);
    this.proxyClass = checkNotNull(proxyClass);
    destroyed = false;
  }

  /**
   * Closes the native handle, so that it can no longer be accessed, and performs a clean action,
   * passing the native handle value.
   *
   *<p>If native handle is not valid, does nothing.
   *
   * <p>This method is idempotent.
   */
  @Override
  public void clean() {
    if (destroyed) {
      return;
    }

    destroyed = true;

    // Do not perform the destructor if the native handle is not valid.
    if (!nativeHandle.isValid()) {
      return;
    }

    long handle = nativeHandle.get();

    // Close the native handle.
    nativeHandle.close();

    // Perform the clean action.
    cleanFunction.accept(handle);
  }

  @Override
  public Optional<Class<?>> resourceType() {
    return Optional.of(proxyClass);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("proxyClass", proxyClass)
        .add("destroyed", destroyed)
        .add("nativeHandle", nativeHandle)
        .toString();
  }
}
