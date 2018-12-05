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

import static com.exonum.binding.storage.indices.MapEntries.putAll;
import static com.exonum.binding.storage.indices.TestStorageItems.K1;
import static com.exonum.binding.storage.indices.TestStorageItems.K2;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.storage.indices.TestStorageItems.V3;
import static com.exonum.binding.storage.indices.TestStorageItems.V4;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.TestProtoMessages.Id;
import com.exonum.binding.storage.indices.TestProtoMessages.Point;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MapIndexProxyIntegrationTest
    extends BaseIndexProxyTestable<MapIndexProxy<String, String>> {

  private static final String MAP_NAME = "test_map";

  @Test
  void newInstanceStoringProtobufMessages() throws CloseFailuresException {
    try (Cleaner c = new Cleaner()) {
      Fork view = database.createFork(c);
      MapIndex<Id, Point> map = MapIndexProxy.newInstance(MAP_NAME, view, Id.class, Point.class);

      // Create a key-value pair of protobuf messages.
      Id id = Id.newBuilder()
          .setId("point 1")
          .build();

      Point point = Point.newBuilder()
          .setX(1)
          .setY(-1)
          .build();

      map.put(id, point);

      // Check that the map contains these messages.
      assertThat(map.get(id), equalTo(point));
    }
  }

  @Test
  void containsKeyShouldReturnFalseIfNoSuchKey() {
    runTestWithView(database::createSnapshot,
        (map) -> assertFalse(map.containsKey(K1))
    );
  }

  @Test
  void containsKeyShouldThrowIfNullKey() {
    assertThrows(NullPointerException.class, () -> runTestWithView(database::createSnapshot,
        (map) -> map.containsKey(null)
    ));
  }

  @Test
  void containsKeyShouldReturnTrueIfHasMappingForKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(K1, V1);
      assertTrue(map.containsKey(K1));
      assertFalse(map.containsKey(K2));
    });
  }

  @Test
  void getShouldReturnSuccessfullyPutValueSingleByteKey() {
    runTestWithView(database::createFork, (map) -> {
      String key = "k";
      String value = V1;

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  void getShouldReturnSuccessfullyPutValueThreeByteKey() {
    runTestWithView(database::createFork, (map) -> {
      String key = "key";
      String value = V1;

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  void putShouldOverwritePreviousValue() {
    runTestWithView(database::createFork, (map) -> {
      String key = "key";

      map.put(key, V1);
      map.put(key, V2);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(V2));
    });
  }

  @Test
  void putAllInEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      ImmutableMap<String, String> source = ImmutableMap.of(
          "k1", V1,
          "k2", V2,
          "k3", V3
      );

      map.putAll(source);

      // Check that the map contains all items
      for (Map.Entry<String, String> entry : source.entrySet()) {
        String key = entry.getKey();
        assertTrue(map.containsKey(key));
        assertThat(map.get(key), equalTo(entry.getValue()));
      }
    });
  }

  @Test
  void putAllOverwritesExistingMappings() {
    runTestWithView(database::createFork, (map) -> {
      // Initialize the map with some entries.
      map.putAll(ImmutableMap.of(
          K1, V1,
          K2, V2
      ));


      ImmutableMap<String, String> replacementEntries = ImmutableMap.of(
          K1, V3,
          K2, V4
      );

      map.putAll(replacementEntries);

      // Check that the map contains new items, not the initial.
      for (Map.Entry<String, String> entry : replacementEntries.entrySet()) {
        String key = entry.getKey();
        assertTrue(map.containsKey(key));
        assertThat(map.get(key), equalTo(entry.getValue()));
      }
    });
  }

  @Test
  void getShouldReturnSuccessfullyPutEmptyValue() {
    runTestWithView(database::createFork, (map) -> {
      String key = K1;
      String value = "";

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  void getShouldReturnSuccessfullyPutValueByEmptyKey() {
    runTestWithView(database::createFork, (map) -> {
      String key = "";
      String value = V1;

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  void putShouldFailWithSnapshot() {
    runTestWithView(database::createSnapshot, (map) -> {
      assertThrows(UnsupportedOperationException.class,
          () -> map.put(K1, V1));
    });
  }

  @Test
  void getShouldReturnNullIfNoSuchValueInFork() {
    runTestWithView(database::createFork, (map) -> {
      String value = map.get(K1);

      assertNull(value);
    });
  }

  @Test
  void getShouldReturnNullIfNoSuchValueInEmptySnapshot() {
    runTestWithView(database::createSnapshot, (map) -> {
      String value = map.get(K1);

      assertNull(value);
    });
  }

  @Test
  void putPrefixKeys() {
    runTestWithView(database::createFork, (map) -> {
      String fullKey = "A long key to take prefixes of";

      // Generate a stream of key-value pairs, where each key is a prefix of the longest key:
      // 'A' -> V1
      // 'A ' -> V2
      // 'A l' -> V3
      // …
      Stream<String> keys = IntStream.range(1, fullKey.length())
          .boxed()
          .map(size -> prefix(fullKey, size));
      Stream<String> values = TestStorageItems.values.stream();
      List<MapEntry<String, String>> entries = Streams.zip(keys, values, MapEntry::valueOf)
          .collect(Collectors.toList());

      // Shuffle so that we don't add in a certain order.
      Collections.shuffle(entries);

      // Add them to the map
      putAll(map, entries);

      // Check that each key maps to the correct value.
      for (MapEntry<String, String> e : entries) {
        String key = e.getKey();
        assertTrue(map.containsKey(key));

        String value = map.get(key);
        String expectedValue = e.getValue();
        assertThat(value, equalTo(expectedValue));
      }
    });
  }

  @Test
  void removeSuccessfullyPutValue() {
    runTestWithView(database::createFork, (map) -> {
      String key = K1;

      map.put(key, V1);
      map.remove(key);

      String storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  void keysShouldReturnEmptyIterIfNoEntries() {
    runTestWithView(database::createSnapshot, (map) -> {
      Iterator<String> iterator = map.keys();

      assertFalse(iterator.hasNext());
    });
  }

  @Test
  void keysShouldReturnIterWithAllKeys() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createSortedMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.keys();
      List<String> keysFromIter = ImmutableList.copyOf(iterator);
      List<String> keysInMap = MapEntries.extractKeys(entries);

      assertThat(keysFromIter, equalTo(keysInMap));
    });
  }

  @Test
  void keysIterNextShouldFailIfThisMapModifiedAfterNext() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.keys();
      iterator.next();
      map.put("new key", "new value");

      assertThrows(ConcurrentModificationException.class, iterator::next);
    });
  }

  @Test
  void keysIterNextShouldFailIfThisMapModifiedBeforeNext() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.keys();
      map.put("new key", "new value");
      assertThrows(ConcurrentModificationException.class, iterator::next);
    });
  }

  @Test
  void keysIterNextShouldFailIfOtherIndexModified() {
    runTestWithView(database::createFork, (view, map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.keys();
      iterator.next();

      MapIndexProxy<String, String> otherMap = createMap("other_map", view);
      otherMap.put("new key", "new value");

      assertThrows(ConcurrentModificationException.class, iterator::next);
    });
  }

  @Test
  void valuesShouldReturnEmptyIterIfNoEntries() {
    runTestWithView(database::createSnapshot, (map) -> {
      Iterator<String> iterator = map.values();
      assertFalse(iterator.hasNext());
    });
  }

  @Test
  void valuesShouldReturnIterWithAllValues() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createSortedMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.values();
      List<String> valuesFromIter = ImmutableList.copyOf(iterator);
      List<String> valuesInMap = MapEntries.extractValues(entries);

      assertThat(valuesFromIter, equalTo(valuesInMap));
    });
  }

  @Test
  void entriesShouldReturnIterWithAllValues() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createSortedMapEntries(3);
      putAll(map, entries);

      Iterator<MapEntry<String, String>> iterator = map.entries();
      List<MapEntry<String, String>> iterEntries = ImmutableList.copyOf(iterator);

      assertThat(iterEntries, equalTo(entries));
    });
  }

  @Test
  void clearEmptyFork() {
    runTestWithView(database::createFork, MapIndexProxy::clear);  // no-op
  }

  @Test
  void clearSnapshotMustFail() {
    runTestWithView(database::createSnapshot, (m) -> {
      assertThrows(UnsupportedOperationException.class,
          m::clear);
    });
  }

  @Test
  void clearSingleItemFork() {
    runTestWithView(database::createFork, (map) -> {
      String key = K1;
      map.put(key, V1);

      map.clear();

      String storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  void clearSingleItemByEmptyKey() {
    runTestWithView(database::createFork, (map) -> {
      String key = "";
      map.put(key, V1);

      map.clear();

      String storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  void clearMultipleItemFork() {
    runTestWithView(database::createFork, (map) -> {
      byte numOfEntries = 5;
      List<MapEntry<String, String>> entries = createMapEntries(numOfEntries);

      putAll(map, entries);

      map.clear();

      // Check there are no entries left.
      for (MapEntry<String, String> e : entries) {
        String storedValue = map.get(e.getKey());
        assertNull(storedValue);
      }
    });
  }

  @Test
  void isEmptyShouldReturnTrueForEmptyMap() {
    runTestWithView(database::createSnapshot, (map) -> assertTrue(map.isEmpty()));
  }

  @Test
  void isEmptyShouldReturnFalseForNonEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      map.put(K1, V1);

      assertFalse(map.isEmpty());
    });
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
      Consumer<MapIndexProxy<String, String>> mapTest) {
    runTestWithView(viewFactory, (ignoredView, map) -> mapTest.accept(map));
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
      BiConsumer<View, MapIndexProxy<String, String>> mapTest) {
    try (Cleaner cleaner = new Cleaner()) {
      View view = viewFactory.apply(cleaner);
      MapIndexProxy<String, String> map = createMap(MAP_NAME, view);

      mapTest.accept(view, map);
    } catch (CloseFailuresException e) {
      throw new AssertionError("Unexpected exception", e);
    }
  }

  @Override
  MapIndexProxy<String, String> create(String name, View view) {
    return createMap(name, view);
  }

  @Override
  Object getAnyElement(MapIndexProxy<String, String> index) {
    return index.get(K1);
  }

  private static MapIndexProxy<String, String> createMap(String name, View view) {
    return MapIndexProxy.newInstance(name, view, StandardSerializers.string(),
        StandardSerializers.string());
  }

  private static String prefix(String source, int prefixSize) {
    checkArgument(prefixSize <= source.length());
    return source.substring(0, prefixSize);
  }

  /**
   * Creates `numOfEntries` map entries: [('a', 'v1'), ('b', 'v2'), … ('z', 'vN+1')].
   */
  private static List<MapEntry<String, String>> createMapEntries(int numOfEntries) {
    return createSortedMapEntries(numOfEntries);
  }

  /**
   * Creates `numOfEntries` map entries, sorted by key:
   * [('a', 'v1'), ('b', 'v2'), … ('z', 'vN+1')].
   */
  private static List<MapEntry<String, String>> createSortedMapEntries(int numOfEntries) {
    assert (numOfEntries < 'z' - 'a');
    List<MapEntry<String, String>> l = new ArrayList<>(numOfEntries);
    for (int i = 0; i < numOfEntries; i++) {
      String key = Character.toString((char) ('a' + i));
      String value = "v" + (i + 1);
      l.add(MapEntry.valueOf(key, value));
    }
    return l;
  }
}
