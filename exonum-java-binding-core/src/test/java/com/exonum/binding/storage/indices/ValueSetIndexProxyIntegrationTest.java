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
import static com.exonum.binding.storage.indices.TestStorageItems.V9;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.View;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedBytes;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValueSetIndexProxyIntegrationTest
    extends BaseIndexProxyTestable<ValueSetIndexProxy<String>> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String VALUE_SET_NAME = "test_value_set";

  @Test
  public void addSingleElement() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);
      assertTrue(set.contains(V1));
    });
  }

  @Test
  public void addFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.add(V1);
    });
  }

  @Test
  public void clearEmptyHasNoEffect() {
    runTestWithView(database::createFork, ValueSetIndexProxy::clear);
  }

  @Test
  public void clearNonEmptyRemovesAllElements() {
    runTestWithView(database::createFork, (set) -> {
      List<String> elements = TestStorageItems.values.subList(0, 3);

      elements.forEach(set::add);

      set.clear();

      elements.forEach(
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
    runTestWithView(database::createSnapshot, (set) -> assertFalse(set.contains(V2)));
  }

  @Test
  public void doesNotContainAbsentElement() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      assertFalse(set.contains(V2));
    });
  }

  @Test
  public void doesNotContainElementsByHashWhenEmpty() {
    runTestWithView(database::createSnapshot, (set) -> {
      HashCode valueHash = getHashOf(V2);
      assertFalse(set.containsByHash(valueHash));
    });
  }

  @Test
  public void containsByHash() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      HashCode valueHash = getHashOf(V1);
      assertTrue(set.containsByHash(valueHash));
    });
  }

  @Test
  public void doesNotContainAbsentElementsByHash() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      HashCode otherValueHash = getHashOf(V2);
      assertFalse(set.containsByHash(otherValueHash));
    });
  }

  @Test
  public void testHashesIter() {
    runTestWithView(database::createFork, (set) -> {
      List<String> elements = TestStorageItems.values;

      elements.forEach(set::add);

      Iterator<HashCode> iter = set.hashes();
      List<HashCode> iterHashes = ImmutableList.copyOf(iter);
      List<HashCode> expectedHashes = getOrderedHashes(elements);

      // Check that the hashes appear in lexicographical order,
      // and are equal to the expected.
      assertThat(iterHashes, equalTo(expectedHashes));
    });
  }

  @Test
  public void testIterator() {
    runTestWithView(database::createFork, (set) -> {
      List<String> elements = TestStorageItems.values;

      elements.forEach(set::add);

      Iterator<ValueSetIndexProxy.Entry<String>> iterator = set.iterator();
      List<ValueSetIndexProxy.Entry<String>> entriesFromIter = ImmutableList.copyOf(iterator);
      List<ValueSetIndexProxy.Entry<String>> entriesExpected = getOrderedEntries(elements);

      assertThat(entriesFromIter, equalTo(entriesExpected));
    });
  }

  private static List<HashCode> getOrderedHashes(List<String> elements) {
    return getOrderedEntries(elements).stream()
        .map(ValueSetIndexProxy.Entry::getHash)
        .collect(Collectors.toList());
  }

  private static List<ValueSetIndexProxy.Entry<String>> getOrderedEntries(List<String> elements) {
    return elements.stream()
        .map(value -> ValueSetIndexProxy.Entry.from(getHashOf(value), value))
        .sorted((e1, e2) -> UnsignedBytes.lexicographicalComparator()
            .compare(e1.getHash().asBytes(), e2.getHash().asBytes()))
        .collect(Collectors.toList());
  }

  @Test
  public void removesAddedElement() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      set.remove(V1);

      assertFalse(set.contains(V1));
    });
  }

  @Test
  public void removeAbsentElementDoesNothing() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      set.remove(V9);

      assertFalse(set.contains(V9));
      assertTrue(set.contains(V1));
    });
  }

  @Test
  public void removeFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.remove(V1);
    });
  }

  @Test
  public void removesAddedElementByHash() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      HashCode valueHash = getHashOf(V1);

      set.removeByHash(valueHash);

      assertFalse(set.contains(V1));
      assertFalse(set.containsByHash(valueHash));
    });
  }

  @Test
  public void removeAbsentElementByHashDoesNothing() {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      HashCode valueHash = getHashOf(V9);
      set.removeByHash(valueHash);

      assertFalse(set.contains(V9));
      assertFalse(set.containsByHash(valueHash));
      assertTrue(set.contains(V1));
    });
  }

  @Test
  public void removeByHashFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.removeByHash(getHashOf(V1));
    });
  }

  /**
   * Creates a view, a value set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewFactory a function creating a database view
   * @param valueSetTest a test to run. Receives the created set as an argument.
   */
  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      Consumer<ValueSetIndexProxy<String>> valueSetTest) {
    runTestWithView(viewFactory,
        (view, valueSetUnderTest) -> valueSetTest.accept(valueSetUnderTest)
    );
  }

  /**
   * Creates a view, a value set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewFactory a function creating a database view
   * @param valueSetTest a test to run. Receives the created view and the set as arguments.
   */
  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      BiConsumer<View, ValueSetIndexProxy<String>> valueSetTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        VALUE_SET_NAME,
        ValueSetIndexProxy::newInstance,
        valueSetTest
    );
  }

  private static HashCode getHashOf(String value) {
    byte[] stringBytes = StandardSerializers.string().toBytes(value);
    return Hashing.defaultHashFunction()
        .hashBytes(stringBytes);
  }

  @Override
  ValueSetIndexProxy<String> create(String name, View view) {
    return ValueSetIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  Object getAnyElement(ValueSetIndexProxy<String> index) {
    return index.contains("v1");
  }
}
