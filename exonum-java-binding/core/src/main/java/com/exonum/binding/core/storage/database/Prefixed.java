/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.util.LibraryLoader;
import com.google.common.annotations.VisibleForTesting;

/**
 * A prefixed database access. It uses a base Access, and adds an address resolution.
 *
 * <p>The Prefixed Access resolves the index addresses by prepending a namespace, followed by
 * a dot ('.'), to the {@linkplain IndexAddress#getName() name part} of the address.
 *
 * <p>This class is a native proxy of the {@code Prefixed} Rust Access.
 */
public final class Prefixed extends AbstractAccess {

  static {
    LibraryLoader.load();
  }

  private final Cleaner cleaner;

  Prefixed(NativeHandle prefixedNativeHandle, boolean canModify, Cleaner cleaner,
      OpenIndexRegistry registry) {
    super(prefixedNativeHandle, canModify, registry);
    this.cleaner = cleaner;
  }

  /**
   * Creates a new Prefixed access given the base database access and the namespace.
   *
   * <p>The new Prefixed access will inherit from the base access its "canModify" property;
   * and use its cleaner and registry of open indexes. When that cleaner gets closed,
   * this access will also be closed.
   *
   * @param namespace the namespace to use
   * @param baseAccess the base database access
   */
  @VisibleForTesting static Prefixed fromAccess(String namespace, AbstractAccess baseAccess) {
    // todo: If it is not used beyond tests, consider removing the _registry sharing_,
    //   since that's the only method that uses (and needs) shared index registry.
    Cleaner cleaner = baseAccess.getCleaner();
    OpenIndexRegistry registry = baseAccess.getOpenIndexes();
    long handle = nativeCreate(namespace, baseAccess.getAccessNativeHandle());
    return fromHandleInternal(handle, baseAccess.canModify(), cleaner, registry);
  }

  /**
   * Creates a Prefixed native peer given the base access native handle.
   * @throws RuntimeException if the base access is unsupported; or if the namespace is not valid
   */
  private static native long nativeCreate(String namespace, long accessNativeHandle);

  /**
   * Creates a new Prefixed access from the native handle. The destructor will be registered
   * in the given cleaner.
   *
   * @param prefixedNativeHandle a handle to the native Prefixed Access
   * @param canModify whether the base access allows modifications
   * @param cleaner a cleaner to destroy the native peer and any dependent objects
   */
  public static Prefixed fromHandle(long prefixedNativeHandle, boolean canModify, Cleaner cleaner) {
    checkNotNull(cleaner);
    // When the base Access is unknown (hidden in native) — use a separate pool of open indexes
    OpenIndexRegistry registry = new OpenIndexRegistry();
    return fromHandleInternal(prefixedNativeHandle, canModify, cleaner, registry);
  }

  /**
   * Expects validated parameters so that it does not throw and registers the destructor
   * properly, which is *required* to prevent leaks.
   */
  private static Prefixed fromHandleInternal(long prefixedNativeHandle, boolean canModify,
      Cleaner cleaner, OpenIndexRegistry registry) {
    NativeHandle handle = new NativeHandle(prefixedNativeHandle);
    ProxyDestructor.newRegistered(cleaner, handle, Prefixed.class,
        /* todo [ECR-4161]: Verify after native implementation! */ Accesses::nativeFree);
    return new Prefixed(handle, canModify, cleaner, registry);
  }

  @Override
  public Cleaner getCleaner() {
    return cleaner;
  }
}
