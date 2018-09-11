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

import static com.exonum.binding.storage.indices.TestStorageItems.K1;
import static com.exonum.binding.storage.indices.TestStorageItems.K9;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.View;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class KeySetIndexProxyIntegrationTest
    extends BaseIndexProxyTestable<KeySetIndexProxy<String>> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String KEY_SET_NAME = "test_key_set";

  @Test
  public void addSingleElement() {
    runTestWithView(database::createFork, (set) -> {
      set.add(K1);
      assertTrue(set.contains(K1));
    });
  }

  @Test
  public void addMultipleElements() {
    runTestWithView(database::createFork, (set) -> {
      List<String> keys = TestStorageItems.keys.subList(0, 3);
      keys.forEach(set::add);
      keys.forEach(
          (k) -> assertTrue(set.contains(k))
      );
    });
  }

  @Test
  public void addFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.add(K1);
    });
  }

  @Test
  public void clearEmptyHasNoEffect() {
    runTestWithView(database::createFork, KeySetIndexProxy::clear);
  }

  @Test
  public void clearNonEmptyRemovesAllElements() {
    runTestWithView(database::createFork, (set) -> {
      List<String> keys = TestStorageItems.keys.subList(0, 3);

      keys.forEach(set::add);

      set.clear();

      keys.forEach(
          (k) -> assertFalse(set.contains(k))
      );
    });
  }

  @Test
  public void clearFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.clear();
    });
  }

  @Test
  public void doesNotContainElementsWhenEmpty() {
    runTestWithView(database::createSnapshot, (set) -> assertFalse(set.contains(K1)));
  }

  @Test
  public void testIterator() {
    runTestWithView(database::createFork, (set) -> {
      List<String> elements = TestStorageItems.keys;

      elements.forEach(set::add);

      Iterator<String> iterator = set.iterator();
      List<String> iterElements = ImmutableList.copyOf(iterator);

      // Check that iterator includes all the elements added
      // and that they appear in lexicographical order (the order of TestStorageItems.keys).
      assertThat(iterElements, equalTo(elements));
    });
  }

  @Test
  public void removesAddedElement() {
    runTestWithView(database::createFork, (set) -> {
      set.add(K1);

      set.remove(K1);

      assertFalse(set.contains(K1));
    });
  }

  @Test
  public void removeNotPresentElementDoesNothing() {
    runTestWithView(database::createFork, (set) -> {
      set.add(K1);

      set.remove(K9);

      assertFalse(set.contains(K9));
      assertTrue(set.contains(K1));
    });
  }

  @Test
  public void removeFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.remove(K1);
    });
  }

  /**
   * Creates a view, a key set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewFactory a function creating a database view
   * @param keySetTest a test to run. Receives the created set as an argument.
   */
  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      Consumer<KeySetIndexProxy<String>> keySetTest) {
    runTestWithView(viewFactory, (view, keySetUnderTest) -> keySetTest.accept(keySetUnderTest));
  }

  /**
   * Creates a view, a key set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewFactory a function creating a database view
   * @param keySetTest a test to run. Receives the created view and the set as arguments.
   */
  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      BiConsumer<View, KeySetIndexProxy<String>> keySetTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        KEY_SET_NAME,
        KeySetIndexProxy::newInstance,
        keySetTest
    );
  }

  @Override
  KeySetIndexProxy<String> create(String name, View view) {
    return KeySetIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  Object getAnyElement(KeySetIndexProxy<String> index) {
    return index.contains("k1");
  }
}
