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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MapIndexProxyIntegrationTest
    extends BaseIndexProxyTestable<MapIndexProxy<String, String>> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String MAP_NAME = "test_map";

  @Test
  public void containsKeyShouldReturnFalseIfNoSuchKey() throws Exception {
    runTestWithView(database::createSnapshot,
        (map) -> assertFalse(map.containsKey(K1))
    );
  }

  @Test(expected = NullPointerException.class)
  public void containsKeyShouldThrowIfNullKey() throws Exception {
    runTestWithView(database::createSnapshot,
        (map) -> map.containsKey(null)
    );
  }

  @Test
  public void containsKeyShouldReturnTrueIfHasMappingForKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      map.put(K1, V1);
      assertTrue(map.containsKey(K1));
      assertFalse(map.containsKey(K2));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutValueSingleByteKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = "k";
      String value = V1;

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutValueThreeByteKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = "key";
      String value = V1;

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void putShouldOverwritePreviousValue() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = "key";

      map.put(key, V1);
      map.put(key, V2);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(V2));
    });
  }

  @Test
  public void putAllInEmptyMap() {
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
  public void putAllOverwritesExistingMappings() {
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
  public void getShouldReturnSuccessfullyPutEmptyValue() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = K1;
      String value = "";

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutValueByEmptyKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = "";
      String value = V1;

      map.put(key, value);

      String storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void putShouldFailWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      map.put(K1, V1);
    });
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInFork() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String value = map.get(K1);

      assertNull(value);
    });
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInEmptySnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      String value = map.get(K1);

      assertNull(value);
    });
  }

  @Test
  public void putPrefixKeys() throws Exception {
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
      List<MapEntry<String, String>> entries = Streams.zip(keys, values, MapEntry::from)
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
  public void removeSuccessfullyPutValue() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = K1;

      map.put(key, V1);
      map.remove(key);

      String storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void keysShouldReturnEmptyIterIfNoEntries() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      Iterator<String> iterator = map.keys();

      assertFalse(iterator.hasNext());
    });
  }

  @Test
  public void keysShouldReturnIterWithAllKeys() throws Exception {
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
  public void keysIterNextShouldFailIfThisMapModifiedAfterNext() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.keys();
      iterator.next();
      map.put("new key", "new value");

      expectedException.expect(ConcurrentModificationException.class);
      iterator.next();
    });
  }

  @Test
  public void keysIterNextShouldFailIfThisMapModifiedBeforeNext() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.keys();
      map.put("new key", "new value");

      expectedException.expect(ConcurrentModificationException.class);
      iterator.next();
    });
  }

  @Test
  public void keysIterNextShouldFailIfOtherIndexModified() throws Exception {
    runTestWithView(database::createFork, (view, map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      Iterator<String> iterator = map.keys();
      iterator.next();

      MapIndexProxy<String, String> otherMap = createMap("other_map", view);
      otherMap.put("new key", "new value");

      expectedException.expect(ConcurrentModificationException.class);
      iterator.next();
    });
  }

  @Test
  public void valuesShouldReturnEmptyIterIfNoEntries() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      Iterator<String> iterator = map.values();
      assertFalse(iterator.hasNext());
    });
  }

  @Test
  public void valuesShouldReturnIterWithAllValues() throws Exception {
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
  public void entriesShouldReturnIterWithAllValues() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createSortedMapEntries(3);
      putAll(map, entries);

      Iterator<MapEntry<String, String>> iterator = map.entries();
      List<MapEntry<String, String>> iterEntries = ImmutableList.copyOf(iterator);

      assertThat(iterEntries, equalTo(entries));
    });
  }

  @Test
  public void clearEmptyFork() throws Exception {
    runTestWithView(database::createFork, MapIndexProxy::clear);  // no-op
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clearSnapshotMustFail() throws Exception {
    runTestWithView(database::createSnapshot, MapIndexProxy::clear);  // boom
  }

  @Test
  public void clearSingleItemFork() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = K1;
      map.put(key, V1);

      map.clear();

      String storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void clearSingleItemByEmptyKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      String key = "";
      map.put(key, V1);

      map.clear();

      String storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void clearMultipleItemFork() throws Exception {
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
      l.add(MapEntry.from(key, value));
    }
    return l;
  }
}
