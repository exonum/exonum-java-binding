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

package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;

/**
 * Represents a view of a database.
 *
 * <p>There are two sub-types:
 * <ul>
 *   <li>A snapshot, which is a <em>read-only</em> view.</li>
 *   <li>A fork, which is a <em>read-write</em> view.</li>
 * </ul>
 *
 * @see Snapshot
 * @see Fork
 */
public abstract class View extends AbstractNativeProxy {

  private final Cleaner cleaner;

  /**
   * Create a new view proxy.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object
   * @param cleaner a cleaner of resources
   */
  View(NativeHandle nativeHandle, Cleaner cleaner) {
    super(nativeHandle);
    this.cleaner = cleaner;
  }

  /**
   *  Returns a native handle of this view.
   *
   *  @throws IllegalStateException if the view is invalid (closed or nullptr)
   */
  public long getViewNativeHandle() {
    return super.getNativeHandle();
  }

  /**
   * Returns the cleaner of this view.
   */
  public Cleaner getCleaner() {
    return cleaner;
  }
}
