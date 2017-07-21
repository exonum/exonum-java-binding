package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.PROOF_MAP_KEY_SIZE;
import static com.exonum.binding.test.TestStorageItems.K1;
import static com.exonum.binding.test.TestStorageItems.V1;
import static com.exonum.binding.test.TestStorageItems.bytes;
import static com.exonum.binding.test.TestStorageItems.values;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.storage.RustIterAdapter;
import com.exonum.binding.storage.StorageIterator;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ProofMapIndexProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final byte[] mapPrefix = bytes("test proof map");

  private static final byte[] PK1 = createProofKey("PK1");
  private static final byte[] PK2 = createProofKey("PK2");
  private static final byte[] PK3 = createProofKey("PK3");

  private static final List<byte[]> proofKeys = asList(PK1, PK2, PK3);

  private Database database;

  @Before
  public void setUp() throws Exception {
    database = new MemoryDb();
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }

  @Test
  public void containsKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);
      assertTrue(map.containsKey(PK1));
    });
  }

  @Test
  public void doesNotContainAbsentKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);
      assertTrue(map.containsKey(PK2));
    });
  }

  @Test
  public void emptyMapDoesNotContainAbsentKey() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      assertFalse(map.containsKey(PK2));
    });
  }

  @Test
  public void containsThrowsIfNullKey() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(NullPointerException.class);
      map.containsKey(null);
    });
  }

  @Test
  public void containsThrowsIfInvalidKey() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(IllegalArgumentException.class);
      map.containsKey(K1);
    });
  }

  @Test
  public void putFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(UnsupportedOperationException.class);
      map.put(PK1, V1);
    });
  }

  @Test
  public void putFailsIfInvalidKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      expectedException.expect(IllegalArgumentException.class);
      map.put(K1, V1);
    });
  }

  @Test
  public void get() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);

      assertThat(map.get(PK1), equalTo(V1));
    });
  }

  @Test
  public void remove() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);
      map.remove(PK1);

      assertNull(map.get(PK1));
      assertFalse(map.containsKey(PK1));
    });
  }

  @Test
  public void removeFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(UnsupportedOperationException.class);
      map.remove(PK1);
    });
  }

  @Test
  public void removeFailsIfInvalidKey() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      expectedException.expect(IllegalArgumentException.class);
      map.remove(K1);
    });
  }

  @Test
  public void keysTest() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries();

      for (MapEntry e : entries) {
        map.put(e.key, e.value);
      }

      try (StorageIterator<byte[]> keysIterator = new RustIterAdapter<>(map.keys())) {
        List<byte[]> keysFromIter = ImmutableList.copyOf(keysIterator);

        assertThat(keysFromIter.size(), equalTo(entries.size()));

        for (byte[] key : keysFromIter) {
          assertTrue(map.containsKey(key));
        }

        for (int i = 0; i < keysFromIter.size(); i++) {
          assertThat(keysFromIter.get(i), equalTo(entries.get(i).key));
        }
      }
    });
  }

  @Test
  public void valuesTest() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries();

      for (MapEntry e : entries) {
        map.put(e.key, e.value);
      }

      try (StorageIterator<byte[]> valuesIterator = new RustIterAdapter<>(map.values())) {
        List<byte[]> valuesFromIter = ImmutableList.copyOf(valuesIterator);

        assertThat(valuesFromIter.size(), equalTo(entries.size()));

        // Values must appear in a lexicographical order of keys
        for (int i = 0; i < valuesFromIter.size(); i++) {
          assertThat(valuesFromIter.get(i), equalTo(entries.get(i).value));
        }
      }
    });
  }

  @Test
  public void clearEmptyHasNoEffect() throws Exception {
    runTestWithView(database::createFork, ProofMapIndexProxy::clear);
  }

  @Test
  public void clearNonEmptyRemovesAllValues() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries();

      for (MapEntry e : entries) {
        map.put(e.key, e.value);
      }

      map.clear();

      for (MapEntry e : entries) {
        assertFalse(map.containsKey(e.key));
      }
    });
  }

  @Test
  public void clearFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(UnsupportedOperationException.class);
      map.clear();
    });
  }

  @Test
  public void closeShallFailIfViewFreedBeforeMap() throws Exception {
    Snapshot view = database.createSnapshot();
    ProofMapIndexProxy map = new ProofMapIndexProxy(mapPrefix, view);

    // Destroy a view before the map.
    view.close();

    expectedException.expect(IllegalStateException.class);
    map.close();
  }

  /**
   * Create a proof key of length 32 with the specified suffix.
   *
   * @param suffix a key suffix. Must be shorter than or equal to 32 bytes in UTF-8.
   * @return a key, starting with zeroes and followed by the specified suffix encoded in UTF-8
   */
  private static byte[] createProofKey(String suffix) {
    byte[] suffixBytes = bytes(suffix);
    checkArgument(suffixBytes.length <= PROOF_MAP_KEY_SIZE);
    byte[] proofKey = new byte[PROOF_MAP_KEY_SIZE];
    System.arraycopy(suffixBytes, 0, proofKey, PROOF_MAP_KEY_SIZE - suffixBytes.length,
        suffixBytes.length);
    return proofKey;
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               Consumer<ProofMapIndexProxy> mapTest) {
    runTestWithView(viewSupplier, (ignoredView, map) -> mapTest.accept(map));
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               BiConsumer<View, ProofMapIndexProxy> mapTest) {
    IndicesTests.runTestWithView(
        viewSupplier,
        mapPrefix,
        ProofMapIndexProxy::new,
        mapTest
    );
  }

  /**
   * Creates `numOfEntries` map entries, sorted by key:
   * [(00…0PK1, V1), (00…0PK2, V2), … (00…0PKi, Vi)].
   */
  private List<MapEntry> createSortedMapEntries() {
    return Streams.zip(proofKeys.stream(), values.stream(), MapEntry::new)
        .collect(Collectors.toList());
  }
}
