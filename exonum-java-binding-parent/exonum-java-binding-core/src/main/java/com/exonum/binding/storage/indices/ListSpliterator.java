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

import com.exonum.binding.storage.database.ModificationCounter;
import com.google.common.annotations.VisibleForTesting;
import java.util.ConcurrentModificationException;
import java.util.Spliterator;
import java.util.function.Consumer;

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
 *  I think we can as we use indices into list: ECR-2849
 */
class ListSpliterator<ElementT> implements Spliterator<ElementT> {

  /** Characteristics of any list spliterator. */
  private static final int ANY_CHARACTERISTICS = NONNULL | ORDERED | SIZED | SUBSIZED;

  @VisibleForTesting
  static final int MIN_SPLITTABLE_SIZE = 2;

  private final ListIndex<ElementT> list;

  /** An index of the next element to read. */
  private long nextIndex;

  /** After-the-last index. */
  private long fence;

  private final ModificationCounter counter;

  private final int characteristics;

  /**
   * The value of the modification counter at the bind time — the start of iteration.
   */
  private Integer initialCounterValue;

  ListSpliterator(ListIndex<ElementT> list, ModificationCounter counter, boolean immutable) {
    this(list, counter, immutable, 0, 0, null);
  }

  private ListSpliterator(ListIndex<ElementT> list, ModificationCounter counter, boolean immutable,
      long nextIndex, long fence, Integer initialCounterValue) {
    this.list = list;
    checkArgument(0 <= fence, "fence (%s) must be non-negative");
    checkPositionIndex(nextIndex, fence);
    this.nextIndex = nextIndex;
    this.fence = fence;
    this.counter = counter;
    characteristics = immutable ? ANY_CHARACTERISTICS | IMMUTABLE
        : ANY_CHARACTERISTICS;
    this.initialCounterValue = initialCounterValue;
  }

  //  todo: can add forEachRemaining and AbstractListIndexProxy#forEachInRange to list that
  //    can avoid performing repetitive operation per each get:
  //    - a check for modifications of the source list
  //    - getNativeHandle
  //    - size checks (a native call to size): [ECR-2817]

  @Override
  public boolean tryAdvance(Consumer<? super ElementT> action) {
    bindOrCheckModifications();
    if (nextIndex < fence) {
      ElementT nextElement = list.get(nextIndex);
      action.accept(nextElement);
      nextIndex++;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Spliterator<ElementT> trySplit() {
    bindOrCheckModifications();
    if (estimateSize() < MIN_SPLITTABLE_SIZE) {
      return null;
    }
    // Create a spliterator covering the prefix of the original subsequence of the list.
    long mid = nextIndex + (fence - nextIndex) / 2;
    Spliterator<ElementT> prefix = new ListSpliterator<>(list, counter,
        hasCharacteristics(IMMUTABLE), nextIndex, mid, initialCounterValue);
    // Make this spliterator cover the suffix of the original subsequence of the list.
    this.nextIndex = mid;

    return prefix;
  }

  @Override
  public long estimateSize() {
    bindOrCheckModifications();
    return fence - nextIndex;
  }

  @Override
  public int characteristics() {
    return characteristics;
  }

  /**
   * Binds this spliterator to the source, initializing the indices to cover the whole list,
   * and setting the modification counter value at bind-time.
   */
  private void bindOrCheckModifications() {
    if (initialCounterValue == null) {
      // Bind this spliterator to the source
      nextIndex = 0;
      fence = list.size();
      initialCounterValue = counter.getCurrentValue();
      return;
    }
    // todo: It also detects non-structural interference (e.g., replacing an element),
    //   isn't it a false-positive? [ECR-2850]
    if (counter.isModifiedSince(initialCounterValue)) {
      throw new ConcurrentModificationException(
          "The source (" + list + ") has been modified since the bind-time");
    }
  }
}
