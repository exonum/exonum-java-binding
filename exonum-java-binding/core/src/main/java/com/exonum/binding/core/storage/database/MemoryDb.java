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

package com.exonum.binding.core.storage.database;

import static com.exonum.binding.core.proxy.NativeHandle.INVALID_NATIVE_HANDLE;

import com.exonum.binding.core.proxy.AbstractCloseableNativeProxy;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.util.LibraryLoader;
import com.google.common.annotations.VisibleForTesting;

/**
 * An in-memory database for testing purposes. It can create both read-only snapshots
 * and read-write forks. The changes made to database forks can be
 * {@linkplain MemoryDb#merge(Fork) applied} to the database state.
 *
 * @see com.exonum.binding.core.service.NodeFake
 */
public final class MemoryDb extends AbstractCloseableNativeProxy implements Database {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a new empty MemoryDb.
   */
  public static MemoryDb newInstance() {
    long nativeHandle = INVALID_NATIVE_HANDLE;
    try {
      nativeHandle = nativeCreate();
      return new MemoryDb(nativeHandle);
    } catch (Throwable t) {
      if (nativeHandle != INVALID_NATIVE_HANDLE) {
        nativeFree(nativeHandle);
      }
      throw t;
    }
  }

  @VisibleForTesting  // Used in native resource manager tests, must not be exported.
  MemoryDb(long nativeHandle) {
    super(nativeHandle, true);
  }

  @Override
  public Snapshot createSnapshot(Cleaner cleaner) {
    long snapshotHandle = nativeCreateSnapshot(getNativeHandle());
    return Snapshot.newInstance(snapshotHandle, cleaner);
  }

  @Override
  public Fork createFork(Cleaner cleaner) {
    long forkHandle = nativeCreateFork(getNativeHandle());
    return Fork.newInstance(forkHandle, cleaner);
  }

  /**
   * Applies the changes from the given fork to the database state. MemoryDb can only
   * merge forks that {@linkplain #createFork(Cleaner) it created itself}.
   *
   * <p>Once this method completes, any indexes created with the fork and the fork itself
   * are closed and cannot be used. Any subsequent operations on these objects will result
   * in {@link IllegalStateException}.
   *
   * <p>TBD: If the fork cannot be applied to the database …
   *
   * @param fork a fork to get changes from
   */
  public void merge(Fork fork) {
    NativeHandle patchHandle = fork.intoPatch();
    nativeMerge(getNativeHandle(), patchHandle.get());
  }

  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate();

  private native long nativeCreateSnapshot(long dbNativeHandle);

  private native long nativeCreateFork(long dbNativeHandle);

  private native void nativeMerge(long dbNativeHandle, long forkNativeHandle);

  private static native void nativeFree(long dbNativeHandle);
}
