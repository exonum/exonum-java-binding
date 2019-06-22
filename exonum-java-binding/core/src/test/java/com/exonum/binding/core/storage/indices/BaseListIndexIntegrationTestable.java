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

import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V3;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Base class for common ListIndex tests.
 */
abstract class BaseListIndexIntegrationTestable
    extends BaseIndexProxyTestable<AbstractListIndexProxy<String>> {

  private static final String LIST_NAME = "test_list";

  @Test
  void addSingleElementToEmptyList() {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String element = l.get(0);

      assertThat(element, equalTo(addedElement));
    });
  }

  @Test
  void addFailsWithSnapshot() {
    assertThrows(UnsupportedOperationException.class,
        () -> runTestWithView(database::createSnapshot, (l) -> l.add(V1)));
  }

  @Test
  void addAllEmptyCollection() {
    runTestWithView(database::createFork, (l) -> {
      l.addAll(Collections.emptyList());

      assertTrue(l.isEmpty());
    });
  }

  @Test
  void addAllEmptyCollectionNonEmptyIndex() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      long initialSize = l.size();

      l.addAll(Collections.emptyList());

      assertThat(l.size(), is(initialSize));
    });
  }

  @Test
  void addAllNonEmptyCollection() {
    runTestWithView(database::createFork, (l) -> {
      List<String> addedElements = asList(V1, V2);
      l.addAll(addedElements);

      assertThat(Math.toIntExact(l.size()), equalTo(addedElements.size()));

      for (int i = 0; i < l.size(); i++) {
        String actual = l.get(i);
        String expected = addedElements.get(i);
        assertThat(actual, equalTo(expected));
      }
    });
  }

  @Test
  void addAllNonEmptyCollectionNonEmptyIndex() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      int initialSize = Math.toIntExact(l.size());

      List<String> addedElements = asList(V1, V2);
      l.addAll(addedElements);

      assertThat(Math.toIntExact(l.size()), equalTo(initialSize + addedElements.size()));

      for (int i = initialSize; i < l.size(); i++) {
        String actual = l.get(i);
        String expected = addedElements.get(i - initialSize);
        assertThat(actual, equalTo(expected));
      }
    });
  }

  @Test
  void addAllCollectionWithFirstNull() {
    runTestWithView(database::createFork, (l) -> {
      List<String> addedElements = asList(null, V2);
      assertThrows(NullPointerException.class, () -> l.addAll(addedElements));
      assertTrue(l.isEmpty());
    });
  }

  @Test
  void addAllCollectionWithSecondNull() {
    runTestWithView(database::createFork, (l) -> {
      List<String> addedElements = asList(V1, null);
      assertThrows(NullPointerException.class, () -> l.addAll(addedElements));
      assertTrue(l.isEmpty());
    });
  }

  @Test
  void addAllNullCollection() {
    runTestWithView(database::createFork, (l) -> {
      assertThrows(NullPointerException.class, () -> l.addAll(null));
    });
  }

  @Test
  void setReplaceFirstSingleElement() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      String replacingElement = "r1";
      l.set(0, replacingElement);

      String element = l.get(0);
      assertThat(element, equalTo(replacingElement));
    });
  }

  @Test
  void setReplaceSecondLastElement() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      String replacingElement = "r2";
      long last = l.size() - 1;
      l.set(last, replacingElement);

      String element = l.get(last);
      assertThat(element, equalTo(replacingElement));
    });
  }

  @Test
  void setReplaceAbsentElement() {
    runTestWithView(database::createFork, (l) -> {
      long invalidIndex = 0;
      String replacingElement = "r2";

      assertThrows(IndexOutOfBoundsException.class, () -> l.set(invalidIndex, replacingElement));
    });
  }

  @Test
  void setWithSnapshot() throws Exception {
    // Initialize the list.
    try (Cleaner cleaner = new Cleaner()) {
      Fork fork = database.createFork(cleaner);
      ListIndex<String> list1 = this.create(LIST_NAME, fork);
      list1.add(V1);
      database.merge(fork);

      Snapshot snapshot = database.createSnapshot(cleaner);
      ListIndex<String> list2 = this.create(LIST_NAME, snapshot);

      // Expect the read-only list to throw an exception in a modifying operation.
      assertThrows(UnsupportedOperationException.class, () -> list2.set(0, V2));
    }
  }

  @Test
  void getLastEmptyList() {
    runTestWithView(database::createFork, (l) -> {
      assertThrows(NoSuchElementException.class, l::getLast);
    });
  }

  @Test
  void getLastSingleElementList() {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String last = l.getLast();

      assertThat(last, equalTo(addedElement));
    });
  }

  @Test
  void getLastTwoElementList() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      String last = l.getLast();

      assertThat(last, equalTo(V2));
    });
  }

  @Test
  void clearEmptyHasNoEffect() {
    runTestWithView(database::createFork, (l) -> {
      l.clear();

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  void clearNonEmptyRemovesAll() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      l.clear();

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  void clearWithSnapshot() {
    runTestWithView(database::createSnapshot, (list) -> {
      assertThrows(UnsupportedOperationException.class, list::clear);
    });
  }

  @Test
  void isEmptyWhenNew() {
    runTestWithView(database::createSnapshot, (l) -> {
      assertTrue(l.isEmpty());
    });
  }

  @Test
  void notEmptyAfterAdd() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      assertFalse(l.isEmpty());
    });
  }

  @Test
  void zeroSizeWhenNew() {
    runTestWithView(database::createSnapshot, (l) -> {
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  void oneSizeAfterAdd() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      assertThat(l.size(), equalTo(1L));
    });
  }

  @Test
  void testIterator() {
    runTestWithView(database::createFork, (l) -> {
      List<String> elements = TestStorageItems.values;

      l.addAll(elements);

      Iterator<String> iterator = l.iterator();
      List<String> iterElements = ImmutableList.copyOf(iterator);

      assertThat(iterElements, equalTo(elements));
    });
  }

  @Test
  void testStream() {
    runTestWithView(database::createFork, (l) -> {
      List<String> elements = TestStorageItems.values;

      l.addAll(elements);

      List<String> streamElements = l.stream()
          .collect(toList());

      assertThat(streamElements, equalTo(elements));
    });
  }

  @Test
  void streamIsLateBinding() {
    runTestWithView(database::createFork, (l) -> {
      // Add some elements
      l.add(V1);
      l.add(V2);

      // Create a stream
      Stream<String> stream = l.stream();

      // Replace the first
      l.set(0, V3);

      // Check the updated values are available, with no exceptions
      String[] streamElements = stream.toArray(String[]::new);
      assertThat(streamElements, arrayContaining(V3, V2));
    });
  }

  @Test
  void streamIsLateBindingInCaseOfStructuralModifications() {
    runTestWithView(database::createFork, (l) -> {
      // Add an element
      l.add(V1);

      // Create a stream
      Stream<String> stream = l.stream();

      // Add a new one
      l.add(V2);

      // Check the updated values are available, with no exceptions
      String[] streamElements = stream.toArray(String[]::new);
      assertThat(streamElements, arrayContaining(V1, V2));
    });
  }

  @Test
  void streamDetectsStructuralModifications() {
    runTestWithView(database::createFork, (l) -> {
      // Add some elements
      l.add(V1);
      l.add(V2);

      // Create a stream that modifies the collection while it is evaluated
      long maxIterations = l.size();
      assertThrows(ConcurrentModificationException.class, () -> l.stream()
          // Perform a structurally-interfering operation on the source list
          .peek(l::add)
          // Limit, so that we don't get infinite number of iterations in case of a broken
          // spliterator
          .limit(maxIterations)
          .count());
    });
  }

  private void runTestWithView(Function<Cleaner, View> viewFactory,
      Consumer<ListIndex<String>> listTest) {
    try (Cleaner cleaner = new Cleaner()) {
      View view = viewFactory.apply(cleaner);
      ListIndex<String> list = this.create(LIST_NAME, view);

      listTest.accept(list);
    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
  }
}
