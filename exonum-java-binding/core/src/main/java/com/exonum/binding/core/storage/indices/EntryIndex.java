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

package com.exonum.binding.core.storage.indices;

import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * An Entry is a database index that may or may not contain a single value.
 *
 * <p>An Entry is analogous to {@link java.util.Optional}, but provides modifying ("destructive")
 * operations when created with a {@link Fork}.
 * Such methods are specified to throw {@link UnsupportedOperationException} if
 * the entry is created with a {@link Snapshot} — a read-only database access.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>When the access goes out of scope, this entry is destroyed. Subsequent use of the closed entry
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <T> the type of an element in this entry
 *
 * @see Access
 */
public interface EntryIndex<T> extends StorageIndex {

  /**
   * Sets a new value of the entry, overwriting the previous value.
   *
   * @param value a value to set. Must not be null.
   * @throws UnsupportedOperationException if the entry is read-only
   * @throws IllegalStateException if the index is invalid
   */
  void set(T value);

  /**
   * Returns true if this entry exists in the database.
   *
   * @throws IllegalStateException if the index is invalid.
   */
  boolean isPresent();

  /**
   * If value is present in the entry, returns it, otherwise,
   * throws {@link NoSuchElementException}.
   *
   * @return a non-null value
   * @throws NoSuchElementException if a value is not present in the Entry
   * @throws IllegalStateException if the index is invalid
   * @throws IllegalArgumentException if the supplied serializer cannot decode the value
   */
  T get();

  /**
   * Removes a value from this entry.
   *
   * @throws UnsupportedOperationException if the entry is read-only.
   * @throws IllegalStateException if the index is invalid
   */
  void remove();

  /**
   * Converts the entry to {@link java.util.Optional}.
   *
   * <p>Be aware that this method represents a state of the entry at the time
   * of calling. And the returned value won't reflect the entry changes:
   * <pre>
   *  {@code
   *    entry.set("foo");
   *    Optional<String> optionalEntry = entry.toOptional();
   *    entry.remove();
   *    optionalEntry.get(); // -> returns "foo"
   *  }
   * </pre>
   *
   * @return {@code Optional.of(value)} if value is present in the entry,
   *        otherwise returns {@code Optional.empty()}
   */
  Optional<T> toOptional();
}
