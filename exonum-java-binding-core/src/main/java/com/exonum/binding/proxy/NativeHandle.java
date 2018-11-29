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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;

/**
 * An implementation-specific handle to the native object. Once closed, can no longer be accessed.
 */
public final class NativeHandle implements AutoCloseable {

  /**
   * A reserved value for an invalid native handle, equal to <code>nullptr</code> in C++.
   */
  public static final long INVALID_NATIVE_HANDLE = 0L;

  private final long nativeHandle;

  private boolean isValid;

  /**
   * Creates new native handle. Validates it's state not allowing to create nullptr handle.
   *
   * @throws IllegalStateException if this native handle is invalid (nullptr)
   */
  public NativeHandle(long nativeHandle) {
    checkState(nativeHandle != INVALID_NATIVE_HANDLE, "This handle is not valid: %s", this);

    this.nativeHandle = nativeHandle;
    this.isValid = true;
  }

  /**
   * Returns a native implementation-specific handle if it may be safely used
   * to access the native object.
   *
   * <p>The returned value shall only be passed as an argument to native methods.
   *
   * <p>Warning: do not cache the return value, as you won't be able to catch use-after-free.
   *
   * @throws IllegalStateException if this native handle is invalid (closed or nullptr)
   */
  public long get() {
    checkValid();
    return nativeHandle;
  }

  @Override
  public void close() {
    if (isValid) {
      invalidate();
    }
  }

  private void checkValid() {
    checkState(isValid, "This handle is not valid: %s", this);
  }

  final boolean isValid() {
    return isValid;
  }

  private void invalidate() {
    isValid = false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("pointer", Long.toHexString(nativeHandle).toUpperCase())
        .toString();
  }
}
