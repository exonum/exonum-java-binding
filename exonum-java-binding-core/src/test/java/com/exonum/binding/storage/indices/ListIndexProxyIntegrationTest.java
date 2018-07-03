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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Contains tests of ListIndexProxy methods that are not present in {@link ListIndex} interface.
 */
public class ListIndexProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private Database database;

  private static final String LIST_NAME = "test_list";

  @Before
  public void setUp() {
    database = MemoryDb.newInstance();
  }

  @After
  public void tearDown() {
    database.close();
  }

  @Test(expected = NoSuchElementException.class)
  public void removeLastEmptyList() {
    runTestWithView(database::createFork, (l) -> {
      String ignored = l.removeLast();
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeLastWithSnapshot() {
    runTestWithView(database::createSnapshot, (l) -> {
      String ignored = l.removeLast();
    });
  }

  @Test
  public void removeLastSingleElementList() {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String last = l.removeLast();

      assertThat(last, equalTo(addedElement));
      assertTrue(l.isEmpty());
    });
  }

  @Test
  public void removeLastTwoElementList() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      String last = l.removeLast();

      assertThat(last, equalTo(V2));
      assertThat(l.size(), equalTo(1L));
      assertThat(l.get(0), equalTo(V1));
    });
  }

  @Test
  public void truncateNonEmptyToZero() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.truncate(0);

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  public void truncateToSameSize() {
    runTestWithView(database::createFork, (l) -> {
      long newSize = 1;
      l.add(V1);
      l.truncate(newSize);

      assertThat(l.size(), equalTo(newSize));
    });
  }

  @Test
  public void truncateToSmallerSize() {
    runTestWithView(database::createFork, (l) -> {
      long newSize = 1;
      l.add(V1);
      l.add(V2);
      l.truncate(newSize);

      assertThat(l.size(), equalTo(newSize));
    });
  }

  @Test
  public void truncateToGreaterSizeHasNoEffect() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      long oldSize = l.size();
      long newSize = 4;
      l.truncate(newSize);

      assertThat(l.size(), equalTo(oldSize));
    });
  }

  @Test
  public void truncateToNegativeSizeThrows() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);

      expectedException.expect(IllegalArgumentException.class);
      long invalidSize = -1;
      l.truncate(invalidSize);
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void truncateWithSnapshot() {
    runTestWithView(database::createSnapshot, (l) -> l.truncate(0L));
  }

  private void runTestWithView(Function<Cleaner, View> viewFactory,
                               Consumer<ListIndexProxy<String>> listTest) {
    runTestWithView(viewFactory, (ignoredView, list) -> listTest.accept(list));
  }

  private void runTestWithView(Function<Cleaner, View> viewFactory,
                               BiConsumer<View, ListIndexProxy<String>> listTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        LIST_NAME,
        ListIndexProxy::newInstance,
        listTest
    );
  }
}
