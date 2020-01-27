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
 * A readonly {@link Fork}-based database Access object.
 *
 * <p>A readonly Fork sees all the changes made to the base Fork, but forbids
 * modifications. That allows safely passing the readonly fork to foreign code.
 *
 * <p>A readonly fork forbids accessing the same index that is being modified in the base
 * fork. Also, accessing an index in the readonly fork will make it inaccessible in the base fork.
 */
public final class ReadonlyFork extends AbstractAccess {

  static {
    LibraryLoader.load();
  }

  private final Cleaner cleaner;

  private ReadonlyFork(NativeHandle nativeHandle, Cleaner cleaner) {
    super(nativeHandle, false);
    this.cleaner = cleaner;
  }

  /**
   * Creates a new ReadonlyFork from the native handle. The destructor will be registered
   * in the given cleaner.
   *
   * @param nativeHandle a handle of the native ReadonlyFork object
   * @param cleaner a cleaner to destroy the native peer and any dependent objects
   */
  public static ReadonlyFork fromHandle(long nativeHandle, Cleaner cleaner) {
    checkNotNull(cleaner);
    return fromHandleInternal(nativeHandle, cleaner);
  }

  @VisibleForTesting static ReadonlyFork fromFork(Fork fork) {
    long baseForkHandle = fork.getAccessNativeHandle();
    long roForkHandle = nativeCreate(baseForkHandle);
    return fromHandleInternal(roForkHandle, fork.getCleaner());
  }

  /**
   * Creates a ReadonlyFork from the Fork (basically, {@code Fork#readonly}.
   */
  private static native long nativeCreate(long baseForkHandle);

  /**
   * Expects validated parameters so that it does not throw and registers the destructor
   * properly, which is *required* to prevent leaks.
   */
  private static ReadonlyFork fromHandleInternal(long roForkNativeHandle, Cleaner cleaner) {
    NativeHandle handle = new NativeHandle(roForkNativeHandle);
    ProxyDestructor.newRegistered(cleaner, handle, ReadonlyFork.class,
        /* todo [ECR-4167]: Verify after native implementation! */ Accesses::nativeFree);
    return new ReadonlyFork(handle, cleaner);
  }

  @Override
  public Cleaner getCleaner() {
    return cleaner;
  }
}
