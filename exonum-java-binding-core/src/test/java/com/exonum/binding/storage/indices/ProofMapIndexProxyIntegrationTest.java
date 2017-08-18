package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.ProofMapContainsMatcher.provesNoMappingFor;
import static com.exonum.binding.storage.indices.ProofMapContainsMatcher.provesThatContains;
import static com.exonum.binding.storage.indices.StoragePreconditions.PROOF_MAP_KEY_SIZE;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkProofKey;
import static com.exonum.binding.storage.indices.TestStorageItems.K1;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.values;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.Hashes;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.primitives.UnsignedBytes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

  private static final byte[] EMPTY_MAP_ROOT_HASH = new byte[Hashes.HASH_SIZE_BYTES];

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
      assertFalse(map.containsKey(PK2));
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

  @Ignore
  @Test
  public void getRootHash_EmptyMap() throws Exception {
    runTestWithView(database::createSnapshot, (map) -> {
      assertThat(map.getRootHash(), equalTo(EMPTY_MAP_ROOT_HASH));
    });
  }

  @Ignore
  @Test
  public void getRootHash_NonEmptyMap() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);

      byte[] rootHash = map.getRootHash();
      assertThat(rootHash, notNullValue());
      assertThat(rootHash.length, equalTo(Hashes.HASH_SIZE_BYTES));
      assertThat(rootHash, not(equalTo(EMPTY_MAP_ROOT_HASH)));
    });
  }

  @Ignore
  @Test
  public void getProof_EmptyMap() throws Exception {
    runTestWithView(database::createSnapshot,
        (map) -> assertThat(map, provesNoMappingFor(PK1))
    );
  }

  @Ignore
  @Test
  public void getProof_SingletonMapContains() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      byte[] key = PK1;
      byte[] value = V1;
      map.put(key, value);

      assertThat(map, provesThatContains(key, value));
    });
  }

  @Ignore
  @Test
  public void getProof_SingletonMapDoesNotContain() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);

      assertThat(map, provesNoMappingFor(PK2));
    });
  }

  @Ignore
  @Test
  public void getProof_MultiEntryMapContains() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries();
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      for (MapEntry e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
    });
  }

  @Ignore
  @Test
  public void getProof_MultiEntryMapDoesNotContain() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries();
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      byte[] allOnes = new byte[PROOF_MAP_KEY_SIZE];
      Arrays.fill(allOnes, UnsignedBytes.checkedCast(0xFF));

      List<byte[]> otherKeys = asList(
          allOnes,  // [11…1]
          createProofKey(""),  // [00…0]
          createProofKey("PK1001"),
          createProofKey("PK1002"),
          createProofKey("PK100500")
      );

      for (byte[] key : otherKeys) {
        assertThat(map, provesNoMappingFor(key));
      }
    });
  }

  @Ignore
  @Test
  // Might take quite a lot of time (validating 257 proofs),
  // but it's an integration test, isn't it? :-)
  //
  // Consider adding a similar test for left-leaning MPT
  public void getProof_MapContainsRightLeaningMaxHeightMpt() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createEntriesForRightLeaningMpt();
      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      for (MapEntry e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
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
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<byte[]> keysIterator = map.keys()) {
        List<byte[]> keysFromIter = ImmutableList.copyOf(keysIterator);

        assertThat(keysFromIter.size(), equalTo(entries.size()));

        for (byte[] key : keysFromIter) {
          assertTrue(map.containsKey(key));
        }

        for (int i = 0; i < keysFromIter.size(); i++) {
          assertThat(keysFromIter.get(i), equalTo(entries.get(i).getKey()));
        }
      }
    });
  }

  @Test
  public void valuesTest() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries();

      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<byte[]> valuesIterator = map.values()) {
        List<byte[]> valuesFromIter = ImmutableList.copyOf(valuesIterator);

        assertThat(valuesFromIter.size(), equalTo(entries.size()));

        // Values must appear in a lexicographical order of keys
        for (int i = 0; i < valuesFromIter.size(); i++) {
          assertThat(valuesFromIter.get(i), equalTo(entries.get(i).getValue()));
        }
      }
    });
  }

  @Test
  public void entriesTest() throws Exception {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry> entries = createSortedMapEntries();

      for (MapEntry e : entries) {
        map.put(e.getKey(), e.getValue());
      }

      try (StorageIterator<MapEntry> entriesIterator = map.entries()) {
        List<MapEntry> entriesFromIter = ImmutableList.copyOf(entriesIterator);

        assertThat(entriesFromIter.size(), equalTo(entries.size()));

        // Entries from the iterator must be present in the map
        for (MapEntry e : entries) {
          assertThat(e.getValue(), equalTo(map.get(e.getKey())));
        }

        // Entries must appear in a lexicographical order of keys
        for (int i = 0; i < entries.size(); i++) {
          MapEntry expected = entries.get(i);
          MapEntry actual = entriesFromIter.get(i);
          assertThat(expected.getKey(), equalTo(actual.getKey()));
          assertThat(expected.getValue(), equalTo(actual.getValue()));
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
        map.put(e.getKey(), e.getValue());
      }

      map.clear();

      for (MapEntry e : entries) {
        assertFalse(map.containsKey(e.getKey()));
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
    return checkProofKey(proofKey);
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

  /**
   * Keys:
   *   00…0000
   *   100…000
   *   0100…00
   *   00100…0
   *   …
   *   00…0100
   *   00…0010
   *   00…0001.
   */
  private static List<MapEntry> createEntriesForRightLeaningMpt() {
    int numKeyBits = Byte.SIZE * PROOF_MAP_KEY_SIZE;
    BitSet keyBits = new BitSet(numKeyBits);
    int numEntries = numKeyBits + 1;
    List<MapEntry> entries = new ArrayList<>(numEntries);
    entries.add(new MapEntry(new byte[PROOF_MAP_KEY_SIZE], V1));

    for (int i = 0; i < numKeyBits; i++) {
      keyBits.set(i);
      byte[] key = createPrefixed(keyBits.toByteArray(), PROOF_MAP_KEY_SIZE);
      byte[] value = values.get(i % values.size());
      entries.add(new MapEntry(key, value));
      keyBits.clear(i);
      assert keyBits.length() == 0;
    }

    return entries;
  }
}
