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

package com.exonum.binding.core.storage.indices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.core.storage.database.IncrementalModificationCounter;
import com.exonum.binding.core.storage.database.ModificationCounter;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class ListSpliteratorTest {

  private static final Consumer<Integer> NULL_CONSUMER = e -> { };

  @Test
  void trySplit_Empty() {
    int[] source = new int[0];
    Spliterator<Integer> spliterator = createSpliteratorOf(source);

    assertNull(spliterator.trySplit());
  }

  @Test
  @DisplayName("trySplit if there are less than minimal splittable size (1 element)")
  void trySplit_OneElement() {
    Spliterator<Integer> spliterator = createSpliteratorOf(new int[1]);

    assertNull(spliterator.trySplit());
  }

  @Test
  void trySplit_MinElements() {
    int[] source = IntStream.range(0, ListSpliterator.MIN_SPLITTABLE_SIZE).toArray();

    Spliterator<Integer> spliterator = createSpliteratorOf(source);

    Spliterator<Integer> prefixSplit = spliterator.trySplit();
    Spliterator<Integer> suffixSplit = spliterator;

    // Check the SUBSIZED requirement: no "disappearing" elements.
    long totalSplitSize = prefixSplit.estimateSize() + suffixSplit.estimateSize();
    assertThat(totalSplitSize).isEqualTo(source.length);

    // Check cannot split further any of them
    assertNull(prefixSplit.trySplit());
    assertNull(suffixSplit.trySplit());

    // Check they preserve order
    int[] combinedResult =
        Stream.concat(StreamSupport.stream(prefixSplit, false),
            StreamSupport.stream(suffixSplit, false))
            .mapToInt(i -> i)
            .toArray();

    assertThat(combinedResult).isEqualTo(source);
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 4, 5, 7, 8, 9, 16, 128})
  @DisplayName("Multiple recursive trySplit split properly")
  void trySplit_splitsWhileSplittable(int size) {
    int[] array = IntStream.range(0, size).toArray();

    Spliterator<Integer> rootSpliterator = createSpliteratorOf(array);

    Stream<Integer> combinedStream = testTrySplitRecursively(rootSpliterator);

    // Check that the resulting array, obtained by splitting the input while it's splittable
    // and then merging pairwise together is the same as the initial array.
    int[] combinedArray = combinedStream
        .mapToInt(i -> i)
        .toArray();
    assertThat(combinedArray).isEqualTo(array);
  }

  /**
   * Splits recursively while splittable then merges the resulting streams together.
   */
  private static Stream<Integer> testTrySplitRecursively(Spliterator<Integer> spliterator) {
    // Get the total size before trySplit
    long totalSize = spliterator.estimateSize();

    Spliterator<Integer> prefixSplit = spliterator.trySplit();
    Spliterator<Integer> suffixSplit = spliterator;

    if (prefixSplit == null) {
      // Cannot split: check that the size remains the same
      assertThat(spliterator.estimateSize()).isEqualTo(totalSize);
      // Accumulate what's left
      return StreamSupport.stream(spliterator, false);
    }

    // Check SIZED + SUBSIZED requirement if have split successfully
    long combinedSize = prefixSplit.estimateSize() + suffixSplit.estimateSize();
    assertThat(combinedSize).isEqualTo(totalSize);

    // Go on splitting both recursively.
    return Stream.concat(
        testTrySplitRecursively(prefixSplit),
        testTrySplitRecursively(suffixSplit)
    );
  }

  @ParameterizedTest
  @MethodSource("bindingOperations")
  void spliteratorFailsFast(Consumer<Spliterator<Integer>> bindingOperation) {
    ListIndex<Integer> list = createListMock();
    ModificationCounter counter = mock(ModificationCounter.class);

    // Create a spliterator
    Spliterator<Integer> spliterator = new ListSpliterator<>(list, counter, true);

    // Perform an operation that binds it to the source
    int counterAtBindTime = 1;
    when(counter.getCurrentValue()).thenReturn(counterAtBindTime);

    bindingOperation.accept(spliterator);

    // Notify of a change in the source
    when(counter.isModifiedSince(counterAtBindTime)).thenReturn(true);

    // Check that subsequent operations fail
    assertHasDetectedModification(spliterator);
  }

  /**
   * Does not include {@link Spliterator#forEachRemaining(Consumer)} because it consumes
   * the spliterator.
   */
  private static List<Consumer<Spliterator<Integer>>> bindingOperations() {
    return Arrays.asList(
        s -> s.tryAdvance(NULL_CONSUMER),
        Spliterator::estimateSize,
        Spliterator::trySplit
    );
  }

  @Test
  void spliteratorRemainsBoundToTheSourceAfterSplit() {
    ListIndex<Integer> list = createListMock();
    when(list.size()).thenReturn(10L);
    ModificationCounter counter = new IncrementalModificationCounter();

    // Create a spliterator
    Spliterator<Integer> spliterator = new ListSpliterator<>(list, counter, true);

    // Split the spliterator into two (binds the original spliterator to the source).
    Spliterator<Integer> other = spliterator.trySplit();

    // Notify of a change in the source
    counter.notifyModified();

    // Check that both spliterators has detected that as a modification
    // (i.e., a new spliterator is already bound to the source when created).
    assertHasDetectedModification(spliterator);
    assertHasDetectedModification(other);
  }

  @Test
  void spliteratorIsLateBinding() {
    ListIndex<Integer> list = createListMock();
    long size = 10;
    when(list.size()).thenReturn(size);
    ModificationCounter counter = new IncrementalModificationCounter();

    // Create a spliterator
    Spliterator<Integer> spliterator = new ListSpliterator<>(list, counter, true);

    // Notify of some modifications
    counter.notifyModified();

    // Test that spliterator is usable despite modifications after initialization but before
    // binding
    assertThat(spliterator.estimateSize()).isEqualTo(size);
    assertTrue(spliterator.tryAdvance(NULL_CONSUMER));

    // Verify that subsequent modifications are detected
    counter.notifyModified();

    assertHasDetectedModification(spliterator);
  }

  @Test
  @DisplayName("spliterator uses the list size at bind time, allowing for structural modifications")
  void spliteratorIsLateBindingUsesProperSize() {
    ListIndex<Integer> list = createListMock();
    long size = 10;
    long initialSize = size - 1;
    when(list.size()).thenReturn(initialSize);
    ModificationCounter counter = new IncrementalModificationCounter();

    // Create a spliterator
    Spliterator<Integer> spliterator = new ListSpliterator<>(list, counter, true);

    // "Add" an element to the list
    when(list.size()).thenReturn(size);
    counter.notifyModified();

    // Test that spliterator is usable despite modifications after initialization but before
    // binding, reporting the correct size
    assertThat(spliterator.estimateSize()).isEqualTo(size);
  }

  private static void assertHasDetectedModification(Spliterator<Integer> spliterator) {
    assertThrows(ConcurrentModificationException.class,
        () -> spliterator.tryAdvance(NULL_CONSUMER));
    assertThrows(ConcurrentModificationException.class,
        () -> spliterator.forEachRemaining(NULL_CONSUMER));
    assertThrows(ConcurrentModificationException.class, spliterator::trySplit);
  }

  @ParameterizedTest
  @ValueSource(longs = {0, 1, 2})
  void estimateSizeAtBindTime(long listSize) {
    ListIndex<Integer> list = createListMock();
    when(list.size()).thenReturn(listSize);
    ModificationCounter counter = mock(ModificationCounter.class);
    Spliterator<Integer> spliterator = new ListSpliterator<>(list, counter, true);

    assertThat(spliterator.estimateSize()).isEqualTo(listSize);
  }

  @ParameterizedTest
  @ValueSource(longs = {1, 2, 3})
  void estimateSizeAfterSingleSuccessfulAdvance(long listSize) {
    ListIndex<Integer> list = createListMock();
    when(list.size()).thenReturn(listSize);
    ModificationCounter counter = mock(ModificationCounter.class);
    Spliterator<Integer> spliterator = new ListSpliterator<>(list, counter, true);

    // Advance the iterator
    spliterator.tryAdvance(NULL_CONSUMER);

    assertThat(spliterator.estimateSize()).isEqualTo(listSize - 1);
  }

  @Test
  void estimateSizeIsZeroAfterSpliteratorIsConsumed() {
    ListIndex<Integer> list = createListMock();
    long listSize = 5;
    when(list.size()).thenReturn(listSize);
    ModificationCounter counter = mock(ModificationCounter.class);
    Spliterator<Integer> spliterator = new ListSpliterator<>(list, counter, true);

    // Advance the iterator
    spliterator.forEachRemaining(NULL_CONSUMER);

    assertThat(spliterator.estimateSize()).isEqualTo(0);
  }

  private static Spliterator<Integer> createSpliteratorOf(int[] source) {
    ListIndex<Integer> list = createListMock();
    lenient().when(list.get(anyLong())).thenAnswer((Answer<Integer>) invocation -> {
      Long index = invocation.getArgument(0);
      return source[Math.toIntExact(index)];
    });
    lenient().when(list.size()).thenReturn((long) source.length);

    ModificationCounter modCounter = mock(ModificationCounter.class);
    lenient().when(modCounter.isModifiedSince(anyInt()))
        .thenReturn(false);

    return new ListSpliterator<>(list, modCounter, true);
  }

  // Don't warn of unchecked assignment of mock of parameterized class
  @SuppressWarnings("unchecked")
  private static ListIndex<Integer> createListMock() {
    return mock(ListIndex.class);
  }
}
