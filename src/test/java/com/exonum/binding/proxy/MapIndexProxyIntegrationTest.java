package com.exonum.binding.proxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MapIndexProxyIntegrationTest {

  static {
    // To have library `libjava_bindings` available by name,
    // add a path to the folder containing it to java.library.path,
    // e.g.: java -Djava.library.path=rust/target/release …
    System.loadLibrary("java_bindings");
    // TODO(dt): Replace with a library loader.
  }

  private static final byte[] mapPrefix = new byte[]{'p'};

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

  // TODO(dt): test evil connect: fail gracefully on use-after-free.

  @Test
  public void getShouldReturnSuccessfullyPutValue() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{1};
      byte[] value = new byte[]{1, 2, 3, 4};

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void putShouldOverwritePreviousValue() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{1};
      byte[] v1 = new byte[]{'v', '1'};
      byte[] v2 = new byte[]{'v', '2'};

      map.put(key, v1);
      map.put(key, v2);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(v2));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutEmptyValue() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{1};
      byte[] value = new byte[]{};

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test
  public void getShouldReturnSuccessfullyPutValueByEmptyKey() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{};
      byte[] value = new byte[]{2};

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    });
  }

  @Test(expected = RuntimeException.class)
  public void putShouldFailWithSnapshot() throws Exception {
    testWithConnect(database::createSnapshot, (map) -> {
      byte[] key = new byte[]{1};
      byte[] value = new byte[]{2};

      map.put(key, value);
    });
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInFork() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{1};
      byte[] value = map.get(key);

      assertNull(value);
    });
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInEmptySnapshot() throws Exception {
    testWithConnect(database::createSnapshot, (map) -> {
      byte[] key = new byte[]{1};
      byte[] value = map.get(key);

      assertNull(value);
    });
  }

  @Test
  public void removeSuccessfullyPutValue() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{1};
      byte[] value = new byte[]{1, 2, 3, 4};

      map.put(key, value);
      map.remove(key);

      byte[] storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void clearEmptyFork() throws Exception {
    testWithConnect(database::createFork, MapIndexProxy::clear);  // no-op
  }

  @Test(expected = RuntimeException.class)
  public void clearSnapshotMustFail() throws Exception {
    testWithConnect(database::createSnapshot, MapIndexProxy::clear);  // boom
  }

  @Test
  public void clearSingleItemFork() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{1};
      byte[] value = new byte[]{1, 2, 3, 4};

      map.put(key, value);
      map.clear();

      byte[] storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void clearSingleItemByEmptyKey() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte[] key = new byte[]{};
      byte[] value = new byte[]{1, 2, 3, 4};

      map.put(key, value);
      map.clear();

      byte[] storedValue = map.get(key);
      assertNull(storedValue);
    });
  }

  @Test
  public void clearMultipleItemFork() throws Exception {
    testWithConnect(database::createFork, (map) -> {
      byte numOfEntries = 5;
      List<Entry> entries = createMapEntries(numOfEntries);

      // Put all entries
      for (Entry e : entries) {
        map.put(e.key, e.value);
      }

      // Clear the map
      map.clear();

      // Check there are no entries left.
      for (Entry e : entries) {
        byte[] storedValue = map.get(e.key);
        assertNull(storedValue);
      }
    });
  }

  private void testWithConnect(Supplier<Connect> viewSupplier,
                               Consumer<MapIndexProxy> mapTest) {
    assert (database != null && database.isValid());
    try (Connect view = viewSupplier.get();
         MapIndexProxy mapUnderTest = new MapIndexProxy(view, mapPrefix)) {
      mapTest.accept(mapUnderTest);
    }
  }

  /**
   * Creates `numOfEntries` map entries: [(0, 1), (1, 2), … (i, i+1)].
   */
  private List<Entry> createMapEntries(byte numOfEntries) {
    assert (numOfEntries < Byte.MAX_VALUE);
    List<Entry> l = new ArrayList<>(numOfEntries);
    for (byte k = 0; k < numOfEntries; k++) {
      byte[] key = bytes(k);
      byte[] value = bytes((byte) (k + 1));
      l.add(new Entry(key, value));
    }
    return l;
  }

  private byte[] bytes(byte... bytes) {
    return bytes;
  }

  private static class Entry {
    byte[] key;
    byte[] value;

    Entry(byte[] key, byte[] value) {
      this.key = key;
      this.value = value;
    }
  }
}
