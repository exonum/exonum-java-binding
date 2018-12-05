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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RustIterAdapterTest {

  private RustIterAdapter<Integer> adapter;

  @Test
  void nextThrowsIfNoNextItem0() {
    adapter = new RustIterAdapter<>(
        new RustIterTestFake(emptyList()));

    assertThrows(NoSuchElementException.class, () -> adapter.next());
  }

  @Test
  void nextThrowsIfNoNextItem1() {
    adapter = new RustIterAdapter<>(
        new RustIterTestFake(singletonList(1)));

    adapter.next();

    assertThrows(NoSuchElementException.class, () -> adapter.next());
  }

  @ParameterizedTest
  @MethodSource("testData")
  void iteratorMustIncludeAllTheItemsFromTheList(List<Integer> underlyingList) {
    // Create an adapter under test, converting a list to rustIter.
    RustIterAdapter<Integer> iterAdapter = new RustIterAdapter<>(
        rustIterMockFromIterable(underlyingList));

    // Use an adapter as Iterator to collect all items in a list
    List<Integer> itemsFromIterAdapter = ImmutableList.copyOf(iterAdapter);

    // check that the lists are the same.
    assertThat(itemsFromIterAdapter, equalTo(underlyingList));
  }

  private static RustIter<Integer> rustIterMockFromIterable(Iterable<Integer> iterable) {
    return new RustIterTestFake(iterable);
  }

  private static List<List<Integer>> testData() {
    return Arrays.asList(
        emptyList(),
        singletonList(1),
        asList(1, 2),
        asList(1, 2, 3),
        asList(1, 2, 3, 4, 5)
    );
  }
}
