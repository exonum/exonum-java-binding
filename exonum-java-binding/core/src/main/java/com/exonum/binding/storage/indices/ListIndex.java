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

package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.database.Fork;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * A list index proxy is a contiguous list of elements.
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>When the corresponding view goes out of scope, this list is destroyed. Subsequent use
 * of the closed list is prohibited and will result in {@link IllegalStateException}.
 *
 * <p>This interface prohibits null elements. All method arguments are non-null by default.
 *
 * @param <T> the type of elements in this list
 */
public interface ListIndex<T> extends StorageIndex, Iterable<T> {

  /**
   * Adds a new element to the end of the list.
   *
   * @param e an element to append to the list
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void add(T e);

  /**
   * Adds all elements from the specified collection to this list.
   *
   * <p>If the collection contains an invalid element, this list is not modified.
   *
   * @param elements elements to add to this list
   * @throws NullPointerException if the collection is null or it contains null elements.
   *                              In this case the collection is not modified.
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void addAll(Collection<? extends T> elements);

  /**
   * Replaces the element at the given index of the list with the specified element.
   *
   * @param index an index of the element to replace
   * @param e an element to add
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void set(long index, T e);

  /**
   * Returns the element at the given index.
   *
   * @param index an index of the element to return
   * @return an element at the given index
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalStateException if this list is not valid
   */
  T get(long index);

  /**
   * Returns the last element of the list.
   *
   * @return the last element of the list
   * @throws NoSuchElementException if the list is empty
   * @throws IllegalStateException if this list is not valid
   */
  T getLast();

  /**
   * Clears the list.
   *
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void clear();

  /**
   * Returns true if the list is empty, false — otherwise.
   *
   * @throws IllegalStateException if this list is not valid
   */
  boolean isEmpty();

  /**
   * Returns the number of elements in the list.
   *
   * @throws IllegalStateException if this list is not valid
   */
  long size();

  /**
   * Returns an iterator over the elements of the list.
   *
   * <p>Any destructive operation on the same {@link Fork} this list uses
   * (but not necessarily on <em>this list</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this list is not valid
   */
  @Override
  Iterator<T> iterator();

  /**
   * Returns a stream of elements in this list.
   * The returned stream is <em>fail-fast</em> and <em>late-binding</em>;
   * the stream can be used as long as the source list is valid.
   *
   * <p>Any destructive operation on the same {@link Fork} this list uses
   * (but not necessarily on <em>this list</em>) will invalidate the corresponding
   * spliterator.
   *
   * @throws IllegalStateException if this list is not valid
   */
  Stream<T> stream();
}
