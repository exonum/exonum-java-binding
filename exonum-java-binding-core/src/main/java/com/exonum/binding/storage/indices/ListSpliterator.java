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

package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.ModificationCounter;
import com.exonum.binding.storage.database.View;
import com.google.common.annotations.VisibleForTesting;
import java.util.ConcurrentModificationException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.LongFunction;

/**
 * A spliterator for list-like Exonum collections.
 *
 * <p>This spliterator is late-binding and fail-fast. It will fail if <em>any</em> collection
 * associated with a given {@link com.exonum.binding.storage.database.Fork} is modified; not only
 * the source list.
 *
 * <p>This spliterator does not support specializations (e.g., {@link Spliterator.OfInt}).
 * If they are ever needed, see the Spliterator in an archived "exonum-serialization" project.
 *
 * @param <ElementT> the type of elements in the corresponding list this spliterator provides
 */
/*
 * todo: Can’t we make a spliterator that is resilient to modifications of unrelated collections?
 *  I think we can as we use indices into list.
 *
 * todo: Shall we use the iterator (from a certain index) that Rust can provide? Is it more
 *   efficient?
 */
class ListSpliterator<ElementT> implements Spliterator<ElementT> {

  /** Characteristics of any list spliterator. */
  private static final int ANY_CHARACTERISTICS = NONNULL | ORDERED | SIZED | SUBSIZED;

  /**
   * Characteristics of a spliterator that is based on
   * a {@link com.exonum.binding.storage.database.Fork}.
   */
  private static final int FORK_CHARACTERISTICS = ANY_CHARACTERISTICS;

  /**
   * Characteristics of a spliterator that is based on
   * a {@link com.exonum.binding.storage.database.Snapshot} — an immutable database view.
   */
  private static final int SNAPSHOT_CHARACTERISTICS = IMMUTABLE | ANY_CHARACTERISTICS;

  // todo: it might be too small, will apply some science later. However, one size will
  //   never fit all as different applications might take vastly different time to process
  //   a single element.
  @VisibleForTesting
  static final int MIN_SPLITTABLE_SIZE = 2;

  /**
   * A function providing elements at a certain position in the list.
   */
  private final LongFunction<ElementT> list;

  /** An index of the next element to read. */
  private long nextIndex;

  /** After-the-last index. */
  private final long fence;
  // todo: we don't _actually_ need a view, we need to know if any modifications that we care
  //   about have occurred.
  private final View view;
  private final ModificationCounter counter;
  private final int characteristics;

  /**
   * The value of the counter at the bind time — the start of iteration.
   */
  private Integer initialCounterValue;

  ListSpliterator(LongFunction<ElementT> listGetter, long nextIndex, long fence, View view,
      ModificationCounter counter) {
    this(listGetter, nextIndex, fence, view, counter, null);
  }

  private ListSpliterator(LongFunction<ElementT> listGetter, long nextIndex, long fence,
      View view, ModificationCounter counter, Integer initialCounterValue) {
    this.list = listGetter;
    checkArgument(0 <= fence, "fence (%s) must be non-negative");
    checkPositionIndex(nextIndex, fence);
    this.nextIndex = nextIndex;
    this.fence = fence;
    this.view = view;
    this.counter = counter;
    characteristics = (view instanceof Fork) ? FORK_CHARACTERISTICS
        : SNAPSHOT_CHARACTERISTICS;
    this.initialCounterValue = initialCounterValue;
  }

  //  todo: can add forEachRemaining and forEachInRange to list that can avoid performing repetitive
  //    operation per each get:
  //    - a check for modifications of the source list
  //    - getNativeHandle
  //    - size checks (a native call to size)

  @Override
  public boolean tryAdvance(Consumer<? super ElementT> action) {
    if (nextIndex < fence) {
      checkNotModified();
      ElementT nextElement = list.apply(nextIndex);
      action.accept(nextElement);
      nextIndex++;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Spliterator<ElementT> trySplit() {
    if (estimateSize() < MIN_SPLITTABLE_SIZE) {
      return null;
    }
    checkNotModified();

    // Create a spliterator covering the prefix of the original subsequence of the list.
    long mid = nextIndex + (fence - nextIndex) / 2;
    Spliterator<ElementT> prefix = new ListSpliterator<>(list, nextIndex, mid, view, counter,
        initialCounterValue);
    // Make this spliterator cover the suffix of the original subsequence of the list.
    this.nextIndex = mid;

    return prefix;
  }

  @Override
  public long estimateSize() {
    // fixme: is it a proper late-binding spliterator if it is configured
    //   with a certain size (= not initialized on demand)?
    checkNotModified();
    return fence - nextIndex;
  }

  @Override
  public int characteristics() {
    return characteristics;
  }

  private void checkNotModified() {
    if (initialCounterValue == null) {
      // Bind this spliterator to the source
      initialCounterValue = counter.getCurrentValue();
      return;
    }
    // todo: It also detects non-structural interference (e.g., replacing an element),
    //   isn't it a false-positive?
    if (counter.isModifiedSince(initialCounterValue)) {
      throw new ConcurrentModificationException("The source has been modified since the bind-time");
    }
  }
}
