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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.storage.database.IncrementalModificationCounter;
import com.exonum.binding.storage.database.ModificationCounter;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListSpliteratorTest {

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
  @ValueSource(ints = {3, 4, 5, 7, 8, 9, 16, 128, 1024, 2048})
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
    View view = mock(Snapshot.class);
    ModificationCounter counter = mock(ModificationCounter.class);

    // Create a spliterator
    int size = 10;
    Spliterator<Integer> spliterator = new ListSpliterator<>(i -> (int) i, 0, size, view, counter);

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
    Consumer<Integer> nullConsumer = e -> { };
    return Arrays.asList(
        s -> s.tryAdvance(nullConsumer),
        Spliterator::estimateSize,
        Spliterator::trySplit
    );
  }

  @Test
  void spliteratorIsBoundToTheSourceAfterSplit() {
    View view = mock(Snapshot.class);
    ModificationCounter counter = new IncrementalModificationCounter();

    // Create a spliterator
    int size = 10;
    Spliterator<Integer> spliterator = new ListSpliterator<>(i -> (int) i, 0, size, view, counter);

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
    View view = mock(Snapshot.class);
    ModificationCounter counter = new IncrementalModificationCounter();

    // Create a spliterator
    int size = 10;
    Spliterator<Integer> spliterator = new ListSpliterator<>(i -> (int) i, 0, size, view, counter);

    // Notify of some modifications
    counter.notifyModified();

    // Test that spliterator is usable despite modifications after initialization but before
    // binding
    assertThat(spliterator.estimateSize()).isEqualTo(size);
    assertTrue(spliterator.tryAdvance(e -> { }));

    // Verify that subsequent modifications are detected
    counter.notifyModified();

    assertHasDetectedModification(spliterator);
  }

  private static void assertHasDetectedModification(Spliterator<Integer> spliterator) {
    assertThrows(ConcurrentModificationException.class, () -> spliterator.tryAdvance(e -> { }));
    assertThrows(ConcurrentModificationException.class,
        () -> spliterator.forEachRemaining(e -> { }));
    assertThrows(ConcurrentModificationException.class, spliterator::trySplit);
  }

  @ParameterizedTest
  @CsvSource({
      "0, 0, 0",
      "0, 1, 1",
      "0, 2, 2",
      "1, 1, 0",
      "1, 2, 1"
  })
  void estimateSize(long origin, long size, long expectedSize) {
    Spliterator<Integer> spliterator = createSpliterator(origin, size);

    assertThat(spliterator.estimateSize()).isEqualTo(expectedSize);
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 5, 6})
  void constructorRejectsInvalidOrigin(long origin) {
    int size = 4;

    assertThrows(IndexOutOfBoundsException.class,
        () -> createSpliterator(origin, size));
  }

  @ParameterizedTest
  @ValueSource(longs = {-2, -1})
  void constructorRejectsInvalidSize(long size) {
    long origin = 2;

    assertThrows(IllegalArgumentException.class,
        () -> createSpliterator(origin, size));
  }

  private static Spliterator<Integer> createSpliteratorOf(int[] source) {
    View view = mock(Snapshot.class);
    ModificationCounter modCounter = mock(ModificationCounter.class);
    lenient().when(modCounter.isModifiedSince(anyInt()))
        .thenReturn(false);

    return new ListSpliterator<>(i -> source[(int) i], 0, source.length, view, modCounter);
  }

  private static Spliterator<Integer> createSpliterator(long origin, long size) {
    // todo: Do we really need a View for diagnostic purposes? Shan't we override a
    //   View#toString then?
    View view = mock(Snapshot.class);
    ModificationCounter modCounter = mock(ModificationCounter.class);
    lenient().when(modCounter.isModifiedSince(anyInt()))
        .thenReturn(false);

    return new ListSpliterator<>(i -> (int) i, origin, size, view, modCounter);
  }

}
