package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.TestStorageItems.K1;
import static com.exonum.binding.storage.indices.TestStorageItems.K2;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
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

  private static final byte[] mapPrefix = bytes("test map");

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
    MapIndexProxy map = new MapIndexProxy(mapPrefix, view);

    // Destroy a view before the map.
    view.close();

    expectedException.expect(IllegalStateException.class);
    map.close();
  }

  @Test
  public void containsKeyShouldReturnFalseIfNoSuchKey() throws Exception {
    runTestWithView(database::createSnapshot,
        (map) -> assertFalse(map.containsKey(K1)));
  }

  @Test(expected = NullPointerException.class)
  public void containsKeyShouldThrowIfNullKey() throws Exception {
    runTestWithView(database::createSnapshot,
        (map) -> map.containsKey(null));
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
  public void getShouldReturnSuccessfullyPutValueSingletonKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = bytes(1);
      byte[] value = bytes(1, 2, 3, 4);

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutValueThreeByteKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = bytes("key");
      byte[] value = bytes("v");

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void putShouldOverwritePreviousValue() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = bytes(1);
      byte[] v1 = bytes("v1");
      byte[] v2 = bytes("v2");

      map.put(key, v1);
      map.put(key, v2);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(v2));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutEmptyValue() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = bytes(1);
      byte[] value = new byte[]{};

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutValueByEmptyKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = new byte[]{};
      byte[] value = bytes(2);

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void putShouldFailWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      byte[] key = bytes(1);
      byte[] value = bytes(2);

      map.put(key, value);
    });
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInFork() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = bytes(1);
      byte[] value = map.get(key);

      assertNull(value);
    });
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInEmptySnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      byte[] key = bytes(1);
      byte[] value = map.get(key);

      assertNull(value);
    });
  }

  @Test
  public void removeSuccessfullyPutValue() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = bytes(1);
      byte[] value = bytes(1, 2, 3, 4);

      map.put(key, value);
      map.remove(key);

      byte[] storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void keysShouldReturnEmptyIterIfNoEntries() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      try (StorageIterator<byte[]> iterator = map.keys()) {
        assertFalse(iterator.hasNext());
      }
    });
  }

  @Test
  public void keysShouldReturnIterWithAllKeys() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries((byte) 3);
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<byte[]> iterator = map.keys()) {
        int i = 0;
        while (iterator.hasNext()) {
          byte[] keyInIter = iterator.next();
          byte[] keyInMap = entries.get(i).getKey();
          assertThat(keyInIter, equalTo(keyInMap));
          i++;
        }
        assertFalse(iterator.hasNext());
      }
    });
  }

  @Test
  public void keysIterNextShouldFailIfThisMapModifiedAfterNext() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createMapEntries((byte) 3);
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<byte[]> iterator = map.keys()) {
        iterator.next();
        map.put(bytes("new key"), bytes("new value"));

        expectedException.expect(ConcurrentModificationException.class);
        iterator.next();
      }
    });
  }

  @Test
  public void keysIterNextShouldFailIfThisMapModifiedBeforeNext() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createMapEntries((byte) 3);
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<byte[]> iterator = map.keys()) {
        map.put(bytes("new key"), bytes("new value"));

        expectedException.expect(ConcurrentModificationException.class);
        iterator.next();
      }
    });
  }

  @Test
  public void keysIterNextShouldFailIfOtherIndexModified() throws Exception {
    runTestWithView(database::createFork, (view, map) -> {
      List<MapEntry> entries = createMapEntries((byte) 3);
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<byte[]> iterator = map.keys()) {
        iterator.next();
        try (MapIndexProxy otherMap = new MapIndexProxy(bytes("other map"), view)) {
          otherMap.put(bytes("new key"), bytes("new value"));
        }

        expectedException.expect(ConcurrentModificationException.class);
        iterator.next();
      }
    });
  }

  @Test
  public void valuesShouldReturnEmptyIterIfNoEntries() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      try (StorageIterator<byte[]> iterator = map.values()) {
        assertFalse(iterator.hasNext());
      }
    });
  }

  @Test
  public void valuesShouldReturnIterWithAllValues() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries((byte) 3);
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<byte[]> iterator = map.values()) {
        int i = 0;
        while (iterator.hasNext()) {
          byte[] valueInIter = iterator.next();
          byte[] valueInMap = entries.get(i).getValue();
          assertThat(valueInIter, equalTo(valueInMap));
          i++;
        }
        assertFalse(iterator.hasNext());
      }
    });
  }

  @Test
  public void entriesShouldReturnIterWithAllValues() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries((byte) 3);
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<MapEntry> iterator = map.entries()) {
        List<MapEntry> iterEntries = ImmutableList.copyOf(iterator);

        assertThat(iterEntries.size(), equalTo(entries.size()));

        for (MapEntry e : iterEntries) {
          assertThat(map.get(e.getKey()), equalTo(e.getValue()));
        }

        for (int i = 0; i < entries.size(); i++) {
          MapEntry expected = entries.get(i);
          MapEntry actual = iterEntries.get(i);
          assertThat(actual.getKey(), equalTo(expected.getKey()));
          assertThat(actual.getValue(), equalTo(expected.getValue()));
        }
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
      byte[] key = bytes(1);
      byte[] value = bytes(1, 2, 3, 4);

      map.put(key, value);
      map.clear();

      byte[] storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void clearSingleItemByEmptyKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = new byte[]{};
      byte[] value = bytes(1, 2, 3, 4);

      map.put(key, value);
      map.clear();

      byte[] storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void clearMultipleItemFork() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte numOfEntries = 5;
      List<MapEntry> entries = createMapEntries(numOfEntries);

      // Put all entries
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      // Clear the map
      map.clear();

      // Check there are no entries left.
      for (MapEntry e : entries) {
        byte[] storedValue = map.get(e.getKey());
        assertNull(storedValue);
      }
    });
  }

  private static void runTestWithView(Supplier<View> viewSupplier,
                                      Consumer<MapIndexProxy> mapTest) {
    runTestWithView(viewSupplier, (ignoredView, map) -> mapTest.accept(map));
  }

  private static void runTestWithView(Supplier<View> viewSupplier,
                                      BiConsumer<View, MapIndexProxy> mapTest) {
    IndicesTests.runTestWithView(
        viewSupplier,
        mapPrefix,
        MapIndexProxy::new,
        mapTest
    );
  }

  /**
   * Creates `numOfEntries` map entries: [(0, 1), (1, 2), … (i, i+1)].
   */
  private static List<MapEntry> createMapEntries(byte numOfEntries) {
    return createSortedMapEntries(numOfEntries);
  }

  /**
   * Creates `numOfEntries` map entries, sorted by key:
   * [(0, 1), (1, 2), … (i, i+1)].
   */
  private static List<MapEntry> createSortedMapEntries(byte numOfEntries) {
    assert (numOfEntries < Byte.MAX_VALUE);
    List<MapEntry> l = new ArrayList<>(numOfEntries);
    for (byte k = 0; k < numOfEntries; k++) {
      byte[] key = bytes(k);
      byte[] value = bytes((byte) (k + 1));
      l.add(new MapEntry(key, value));
    }
    return l;
  }
}
