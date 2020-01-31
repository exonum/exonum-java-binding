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
import com.exonum.binding.core.util.LibraryLoader;
import com.google.common.annotations.VisibleForTesting;

/**
 * A readonly, "erased", database Access object.
 *
 * <p>This class primarily exists to support readonly forks and snapshots, but also supports
 * other readonly accesses.
 *
 * <h2>Readonly Forks</h2>
 *
 * <p>A readonly Fork sees all the changes made to the base Fork, but forbids
 * modifications. That allows safely passing the readonly fork to foreign code.
 *
 * <p>A readonly fork forbids accessing the same index that is being modified in the base
 * fork. Also, accessing an index in the readonly fork will make it inaccessible in the base fork.
 */
public final class RoErasedAccess extends AbstractAccess {

  static {
    LibraryLoader.load();
  }

  private final Cleaner cleaner;

  private RoErasedAccess(NativeHandle nativeHandle, Cleaner cleaner) {
    super(nativeHandle, false);
    this.cleaner = cleaner;
  }

  /**
   * Creates a new erased readonly access from the native handle. The destructor will be registered
   * in the given cleaner.
   *
   * @param nativeHandle a handle of the native readonly ErasedAccess object
   * @param cleaner a cleaner to destroy the native peer and any dependent objects
   */
  public static RoErasedAccess fromHandle(long nativeHandle, Cleaner cleaner) {
    checkNotNull(cleaner);
    return fromHandleInternal(nativeHandle, cleaner);
  }

  /**
   * Creates a readonly access from the given one.
   */
  @VisibleForTesting static RoErasedAccess fromRawAccess(AbstractAccess access) {
    long baseForkHandle = access.getAccessNativeHandle();
    long roForkHandle = nativeAsReadonly(baseForkHandle);
    return fromHandleInternal(roForkHandle, access.getCleaner());
  }

  /**
   * Creates a readonly access from the given one (basically, {@code AsReadonly#as_readonly}.
   */
  private static native long nativeAsReadonly(long baseAccessHandle);

  /**
   * Expects validated parameters so that it does not throw and registers the destructor
   * properly, which is *required* to prevent leaks.
   */
  private static RoErasedAccess fromHandleInternal(long erasedNativeHandle, Cleaner cleaner) {
    NativeHandle handle = new NativeHandle(erasedNativeHandle);
    ProxyDestructor.newRegistered(cleaner, handle, RoErasedAccess.class, Accesses::nativeFree);
    return new RoErasedAccess(handle, cleaner);
  }

  @Override
  public Cleaner getCleaner() {
    return cleaner;
  }
}
