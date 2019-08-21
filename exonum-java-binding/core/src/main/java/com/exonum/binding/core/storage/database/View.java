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

import com.exonum.binding.core.proxy.AbstractNativeProxy;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.StorageIndex;
import java.util.Optional;

/**
 * Represents a view of the database.
 *
 * <p>There are two sub-types:
 * <ul>
 *   <li>A snapshot, which is a <em>read-only</em> and immutable view.</li>
 *   <li>A fork, which is a <em>read-write</em> view.</li>
 * </ul>
 *
 * <p>As in some cases the clients need to detect any changes made to a database, a view also
 * holds a modification counter, which any clients changing the database state must notify.
 *
 * @see Snapshot
 * @see Fork
 */
public abstract class View extends AbstractNativeProxy {

  private final Cleaner cleaner;
  private final OpenIndexRegistry indexRegistry = new OpenIndexRegistry();
  private final boolean canModify;

  /**
   * Create a new view proxy.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object
   * @param cleaner a cleaner of resources
   * @param canModify if the view allows modifications
   */
  View(NativeHandle nativeHandle, Cleaner cleaner, boolean canModify) {
    super(nativeHandle);
    this.cleaner = cleaner;
    this.canModify = canModify;
  }

  /**
   * Returns true if this view allows modifications to the database state; false if it is
   * immutable.
   */
  public boolean canModify() {
    return canModify;
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
   * Finds an open index by the given address.
   *
   * <p><em>This method is for internal use. It is not designed to be used by services,
   * rather by index factories.</em>
   *
   * @param address the index address
   * @return an index with the given address; or {@code Optional.empty()} if no index
   *     with such address was open in this view
   */
  public Optional<StorageIndex> findOpenIndex(IndexAddress address) {
    return indexRegistry.findIndex(address);
  }

  /**
   * Registers a new index created with this view.
   *
   * <p><em>This method is for internal use. It is not designed to be used by services,
   * rather by index factories.</em>
   *
   * @param index a new index to register
   * @throws IllegalArgumentException if the index is already registered
   * @see #findOpenIndex(IndexAddress)
   */
  public void registerIndex(StorageIndex index) {
    indexRegistry.registerIndex(index);
  }

  /**
   * Returns the cleaner of this view.
   */
  public Cleaner getCleaner() {
    return cleaner;
  }
}
