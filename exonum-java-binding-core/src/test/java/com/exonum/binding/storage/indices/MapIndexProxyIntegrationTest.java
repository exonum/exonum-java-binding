package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.MapEntries.putAll;
import static com.exonum.binding.storage.indices.TestStorageItems.K1;
import static com.exonum.binding.storage.indices.TestStorageItems.K2;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MapIndexProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String MAP_NAME = "test_map";

  private Database database;

  @Before
  public void setUp() throws Exception {
    database = new MemoryDb();
  }

  @After
  public void tearDown() throws Exception {
    if (database != null) {
      database.close();
    }
  }

  /**
   * This test verifies that if a client destroys native objects through their proxies
   * in the wrong order, he will get a runtime exception before a (possible) JVM crash.
   */
  @Test
  public void closeShallThrowIfViewFreedBeforeMap() throws Exception {
    Snapshot view = database.createSnapshot();
    MapIndexProxy<String, String> map = createMap(MAP_NAME, view);

    // Destroy a view before the map.
    view.close();

    expectedException.expect(IllegalStateException.class);
    map.close();
  }

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
      try (StorageIterator<String> iterator = map.keys()) {
        assertFalse(iterator.hasNext());
      }
    });
  }

  @Test
  public void keysShouldReturnIterWithAllKeys() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createSortedMapEntries(3);
      putAll(map, entries);

      try (StorageIterator<String> iterator = map.keys()) {
        List<String> keysFromIter = ImmutableList.copyOf(iterator);
        List<String> keysInMap = MapEntries.extractKeys(entries);

        assertThat(keysFromIter, equalTo(keysInMap));
      }
    });
  }

  @Test
  public void keysIterNextShouldFailIfThisMapModifiedAfterNext() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      try (StorageIterator<String> iterator = map.keys()) {
        iterator.next();
        map.put("new key", "new value");

        expectedException.expect(ConcurrentModificationException.class);
        iterator.next();
      }
    });
  }

  @Test
  public void keysIterNextShouldFailIfThisMapModifiedBeforeNext() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      try (StorageIterator<String> iterator = map.keys()) {
        map.put("new key", "new value");

        expectedException.expect(ConcurrentModificationException.class);
        iterator.next();
      }
    });
  }

  @Test
  public void keysIterNextShouldFailIfOtherIndexModified() throws Exception {
    runTestWithView(database::createFork, (view, map) -> {
      List<MapEntry<String, String>> entries = createMapEntries(3);
      putAll(map, entries);

      try (StorageIterator<String> iterator = map.keys()) {
        iterator.next();
        try (MapIndexProxy<String, String> otherMap = createMap("other_map", view)) {
          otherMap.put("new key", "new value");
        }

        expectedException.expect(ConcurrentModificationException.class);
        iterator.next();
      }
    });
  }

  @Test
  public void valuesShouldReturnEmptyIterIfNoEntries() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      try (StorageIterator<String> iterator = map.values()) {
        assertFalse(iterator.hasNext());
      }
    });
  }

  @Test
  public void valuesShouldReturnIterWithAllValues() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createSortedMapEntries(3);
      putAll(map, entries);

      try (StorageIterator<String> iterator = map.values()) {
        List<String> valuesFromIter = ImmutableList.copyOf(iterator);
        List<String> valuesInMap = MapEntries.extractValues(entries);

        assertThat(valuesFromIter, equalTo(valuesInMap));
      }
    });
  }

  @Test
  public void entriesShouldReturnIterWithAllValues() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<String, String>> entries = createSortedMapEntries(3);
      putAll(map, entries);

      try (StorageIterator<MapEntry<String, String>> iterator = map.entries()) {
        List<MapEntry<String, String>> iterEntries = ImmutableList.copyOf(iterator);

        assertThat(iterEntries, equalTo(entries));
      }
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

  private static void runTestWithView(Supplier<View> viewSupplier,
                                      Consumer<MapIndexProxy<String, String>> mapTest) {
    runTestWithView(viewSupplier, (ignoredView, map) -> mapTest.accept(map));
  }

  private static void runTestWithView(Supplier<View> viewSupplier,
                                      BiConsumer<View, MapIndexProxy<String, String>> mapTest) {
    try (View view = viewSupplier.get();
         MapIndexProxy<String, String> map = createMap(MAP_NAME, view)) {
      mapTest.accept(view, map);
    }
  }

  private static MapIndexProxy<String, String> createMap(String name, View view) {
    return new MapIndexProxy<>(name, view, StandardSerializers.string(),
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
