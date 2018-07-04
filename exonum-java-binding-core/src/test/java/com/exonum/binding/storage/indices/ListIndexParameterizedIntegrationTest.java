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

import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.test.TestParameters.parameters;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.IndexConstructors.PartiallyAppliedIndexConstructor;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * A test of common ListIndex methods.
 */
@RunWith(Parameterized.class)
public class ListIndexParameterizedIntegrationTest
    extends BaseIndexProxyTestable<AbstractListIndexProxy<String>> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Parameterized.Parameter(0)
  public PartiallyAppliedIndexConstructor<ListIndex<String>> listFactory;

  @Parameterized.Parameter(1)
  public String testName;

  private static final String LIST_NAME = "test_list";

  @Test
  public void addSingleElementToEmptyList() {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String element = l.get(0);

      assertThat(element, equalTo(addedElement));
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addFailsWithSnapshot() {
    runTestWithView(database::createSnapshot, (l) -> l.add(V1));
  }

  @Test
  public void addAllEmptyCollection() {
    runTestWithView(database::createFork, (l) -> {
      l.addAll(Collections.emptyList());

      assertTrue(l.isEmpty());
    });
  }

  @Test
  public void addAllEmptyCollectionNonEmptyIndex() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      long initialSize = l.size();

      l.addAll(Collections.emptyList());

      assertThat(l.size(), equalTo(initialSize));
    });
  }

  @Test
  public void addAllNonEmptyCollection() {
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
  public void addAllNonEmptyCollectionNonEmptyIndex() {
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
  public void addAllCollectionWithFirstNull() {
    runTestWithView(database::createFork, (l) -> {
      List<String> addedElements = asList(null, V2);
      try {
        l.addAll(addedElements);
        fail("Expected NPE");
      } catch (NullPointerException e) {
        assertTrue(l.isEmpty());
      }
    });
  }

  @Test
  public void addAllCollectionWithSecondNull() {
    runTestWithView(database::createFork, (l) -> {
      List<String> addedElements = asList(V1, null);
      try {
        l.addAll(addedElements);
        fail("Expected NPE");
      } catch (NullPointerException e) {
        assertTrue(l.isEmpty());
      }
    });
  }

  @Test
  public void addAllNullCollection() {
    runTestWithView(database::createFork, (l) -> {
      expectedException.expect(NullPointerException.class);
      l.addAll(null);
    });
  }

  @Test
  public void setReplaceFirstSingleElement() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      String replacingElement = "r1";
      l.set(0, replacingElement);

      String element = l.get(0);
      assertThat(element, equalTo(replacingElement));
    });
  }

  @Test
  public void setReplaceSecondLastElement() {
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
  public void setReplaceAbsentElement() {
    runTestWithView(database::createFork, (l) -> {
      long invalidIndex = 0;
      String replacingElement = "r2";

      expectedException.expect(IndexOutOfBoundsException.class);
      l.set(invalidIndex, replacingElement);
    });
  }

  @Test
  public void setWithSnapshot() throws Exception {
    // Initialize the list.
    try (Cleaner cleaner = new Cleaner()) {
      Fork fork = database.createFork(cleaner);
      ListIndex<String> list1 = createList(fork);
      list1.add(V1);
      database.merge(fork);

      Snapshot snapshot = database.createSnapshot(cleaner);
      ListIndex<String> list2 = createList(snapshot);

      // Expect the read-only list to throw an exception in a modifying operation.
      expectedException.expect(UnsupportedOperationException.class);
      list2.set(0, V2);
    }
  }

  @Test(expected = NoSuchElementException.class)
  public void getLastEmptyList() {
    runTestWithView(database::createFork, (l) -> {
      String ignored = l.getLast();
    });
  }

  @Test
  public void getLastSingleElementList() {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String last = l.getLast();

      assertThat(last, equalTo(addedElement));
    });
  }

  @Test
  public void getLastTwoElementList() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      String last = l.getLast();

      assertThat(last, equalTo(V2));
    });
  }

  @Test
  public void clearEmptyHasNoEffect() {
    runTestWithView(database::createFork, (l) -> {
      l.clear();

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  public void clearNonEmptyRemovesAll() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      l.clear();

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clearWithSnapshot() {
    runTestWithView(database::createSnapshot, ListIndex::clear);
  }

  @Test
  public void isEmptyWhenNew() {
    runTestWithView(database::createSnapshot, (l) -> {
      assertTrue(l.isEmpty());
    });
  }

  @Test
  public void notEmptyAfterAdd() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      assertFalse(l.isEmpty());
    });
  }

  @Test
  public void zeroSizeWhenNew() {
    runTestWithView(database::createSnapshot, (l) -> {
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  public void oneSizeAfterAdd() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      assertThat(l.size(), equalTo(1L));
    });
  }

  @Test
  public void testIterator() {
    runTestWithView(database::createFork, (l) -> {
      List<String> elements = TestStorageItems.values;

      l.addAll(elements);

      Iterator<String> iterator = l.iterator();
      List<String> iterElements = ImmutableList.copyOf(iterator);

      assertThat(iterElements, equalTo(elements));
    });
  }

  private void runTestWithView(Function<Cleaner, View> viewFactory,
                               Consumer<ListIndex<String>> listTest) {
    runTestWithView(viewFactory, (ignoredView, list) -> listTest.accept(list));
  }

  private void runTestWithView(Function<Cleaner, View> viewFactory,
                               BiConsumer<View, ListIndex<String>> listTest) {
    try (Cleaner cleaner = new Cleaner()) {
      View view = viewFactory.apply(cleaner);
      ListIndex<String> list = createList(view);

      listTest.accept(view, list);
    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  AbstractListIndexProxy<String> create(String name, View view) {
    return (AbstractListIndexProxy<String>) listFactory.create(name, view);
  }

  @Override
  Object getAnyElement(AbstractListIndexProxy<String> index) {
    return index.get(0L);
  }

  private ListIndex<String> createList(View view) {
    return listFactory.create(LIST_NAME, view);
  }

  @Parameters(name = "{index}: {1}")
  public static Collection<Object[]> testData() {
    return asList(
        parameters(IndexConstructors.from(ListIndexProxy::newInstance), "ListIndex"),
        parameters(IndexConstructors.from(ProofListIndexProxy::newInstance), "ProofListIndex")
    );
  }
}
