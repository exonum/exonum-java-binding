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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.database.View;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Contains tests of ListIndexProxy methods that are not present in {@link ListIndex} interface.
 */
class ListIndexProxyIntegrationTest extends BaseListIndexIntegrationTestable {

  private static final String LIST_NAME = "test_list";

  @Override
  ListIndexProxy<String> create(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  Object getAnyElement(AbstractListIndexProxy<String> index) {
    return index.get(0L);
  }

  @Test
  void removeLastEmptyList() {
    runTestWithView(database::createFork, (l) -> {
      assertThrows(NoSuchElementException.class, l::removeLast);
    });
  }

  @Test
  void removeLastWithSnapshot() {
    runTestWithView(database::createSnapshot, (l) -> {
      assertThrows(UnsupportedOperationException.class, l::removeLast);
    });
  }

  @Test
  void removeLastSingleElementList() {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String last = l.removeLast();

      assertThat(last, equalTo(addedElement));
      assertTrue(l.isEmpty());
    });
  }

  @Test
  void removeLastTwoElementList() {
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
  void truncateNonEmptyToZero() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.truncate(0);

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  void truncateToSameSize() {
    runTestWithView(database::createFork, (l) -> {
      long newSize = 1;
      l.add(V1);
      l.truncate(newSize);

      assertThat(l.size(), equalTo(newSize));
    });
  }

  @Test
  void truncateToSmallerSize() {
    runTestWithView(database::createFork, (l) -> {
      long newSize = 1;
      l.add(V1);
      l.add(V2);
      l.truncate(newSize);

      assertThat(l.size(), equalTo(newSize));
    });
  }

  @Test
  void truncateToGreaterSizeHasNoEffect() {
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
  void truncateToNegativeSizeThrows() {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);


      assertThrows(IllegalArgumentException.class, () -> {
        long invalidSize = -1;
        l.truncate(invalidSize);
      });

    });
  }

  @Test
  void truncateWithSnapshot() {
    runTestWithView(database::createSnapshot, (l) -> {
      assertThrows(UnsupportedOperationException.class,
          () -> l.truncate(0L));
    });
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
