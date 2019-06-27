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

/**
 * A counter of modification events of some objects (e.g., a collection, or a database view).
 * It is updated each time the object notifies of an event. The clients that need
 * to detect modifications must save the current value of the counter, and check if it has changed
 * to determine if the corresponding source object is modified.
 *
 * <p>Implementations must reliably detect up to 4 billion modifications (2^32-1).
 *
 * <p>Implementations are not required to be thread-safe.
 */
public interface ModificationCounter {

  /**
   * Returns true if the counter was modified since the given value (if {@link #notifyModified()}
   * has been invoked); false — otherwise.
   *
   * @param lastValue the last value of the counter
   */
  boolean isModifiedSince(int lastValue);

  /**
   * Returns the current value of the counter. No assumptions must be made on how it changes
   * when a notification is received.
   */
  int getCurrentValue();

  /**
   * Notifies this counter that the source object is modified, updating its current value.
   *
   * @throws IllegalStateException if this counter corresponds to a read-only (immutable) object,
   *     i.e., must reject any modification events
   */
  void notifyModified();
}
