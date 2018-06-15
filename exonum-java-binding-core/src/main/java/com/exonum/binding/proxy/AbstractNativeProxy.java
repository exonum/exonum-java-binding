/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.proxy;

/**
 * A base class of a native proxy.
 */
public abstract class AbstractNativeProxy {

  /** A handle to the native object. */
  protected final NativeHandle nativeHandle;

  protected AbstractNativeProxy(NativeHandle nativeHandle) {
    this.nativeHandle = nativeHandle;
  }

  /**
   * Returns a handle to the native object if it may be safely used to access the native object.
   * Equivalent to {@code nativeHandle.get()}.
   *
   * <p>The returned value shall only be passed as an argument to native methods.
   *
   * <p><strong>Warning:</strong> do not cache the return value, as you won't be able
   * to catch use-after-free.
   *
   * @throws IllegalStateException if the native handle is invalid (closed or nullptr)
   */
  protected long getNativeHandle() {
    return nativeHandle.get();
  }

  /**
   * Returns true if this proxy has a valid native handle.
   */
  protected final boolean isValidHandle() {
    return nativeHandle.isValid();
  }
}
