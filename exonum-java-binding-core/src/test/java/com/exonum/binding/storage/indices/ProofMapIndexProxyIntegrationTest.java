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

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.storage.indices.MapEntries.putAll;
import static com.exonum.binding.storage.indices.ProofMapContainsMatcher.provesNoMappingFor;
import static com.exonum.binding.storage.indices.ProofMapContainsMatcher.provesThatContains;
import static com.exonum.binding.storage.indices.StoragePreconditions.PROOF_MAP_KEY_SIZE;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkProofKey;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.storage.indices.TestStorageItems.V3;
import static com.exonum.binding.storage.indices.TestStorageItems.V4;
import static com.exonum.binding.storage.indices.TestStorageItems.values;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.proofs.map.MapProof;
import com.exonum.binding.storage.proofs.map.MapProofTreePrinter;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.primitives.UnsignedBytes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ProofMapIndexProxyIntegrationTest
    extends BaseIndexProxyTestable<ProofMapIndexProxy<HashCode, String>> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String MAP_NAME = "test_proof_map";

  private static final List<HashCode> PROOF_KEYS = Stream.of(
      Bytes.bytes(0x00),
      Bytes.bytes(0x01),
      Bytes.bytes(0x02),
      Bytes.bytes(0x08),
      Bytes.bytes(0x0f),
      Bytes.bytes(0x10),
      Bytes.bytes(0x20),
      Bytes.bytes(0x80),
      Bytes.bytes(0xf0),
      Bytes.bytes(0xff),
      Bytes.bytes(0x01, 0x01),
      Bytes.bytes(0x01, 0x10),
      Bytes.bytes(0x10, 0x01),
      Bytes.bytes(0x10, 0x10)
  )
      .map(ProofMapIndexProxyIntegrationTest::createRawProofKey)
      .sorted(UnsignedBytes.lexicographicalComparator())
      .map(HashCode::fromBytes)
      .collect(Collectors.toList());

  static final HashCode PK1 = PROOF_KEYS.get(0);
  static final HashCode PK2 = PROOF_KEYS.get(1);
  static final HashCode PK3 = PROOF_KEYS.get(2);

  private static final HashCode INVALID_PROOF_KEY = HashCode.fromString("1234");

  private static final HashCode EMPTY_MAP_ROOT_HASH = HashCode.fromBytes(
      new byte[DEFAULT_HASH_SIZE_BYTES]);

  @Test
  public void containsKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);
      assertTrue(map.containsKey(PK1));
    });
  }

  @Test
  public void doesNotContainAbsentKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);
      assertFalse(map.containsKey(PK2));
    });
  }

  @Test
  public void emptyMapDoesNotContainAbsentKey() {
    runTestWithView(database::createSnapshot, (map) -> {
      assertFalse(map.containsKey(PK2));
    });
  }

  @Test
  public void containsThrowsIfNullKey() {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(NullPointerException.class);
      map.containsKey(null);
    });
  }

  @Test
  public void containsThrowsIfInvalidKey() {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(IllegalArgumentException.class);
      map.containsKey(INVALID_PROOF_KEY);
    });
  }

  @Test
  public void putFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(UnsupportedOperationException.class);
      map.put(PK1, V1);
    });
  }

  @Test
  public void putFailsIfInvalidKey() {
    runTestWithView(database::createFork, (map) -> {
      expectedException.expect(IllegalArgumentException.class);
      map.put(INVALID_PROOF_KEY, V1);
    });
  }

  @Test
  public void putAllInEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      ImmutableMap<HashCode, String> source = ImmutableMap.of(
          PK1, V1,
          PK2, V2
      );

      map.putAll(source);

      // Check that the map contains all items
      for (Map.Entry<HashCode, String> entry : source.entrySet()) {
        HashCode key = entry.getKey();
        assertTrue(map.containsKey(key));
        assertThat(map.get(key), equalTo(entry.getValue()));
      }
    });
  }

  @Test
  public void putAllOverwritingEntries() {
    runTestWithView(database::createFork, (map) -> {
      map.putAll(ImmutableMap.of(
          PK1, V1,
          PK2, V2
      ));

      ImmutableMap<HashCode, String> replacements = ImmutableMap.of(
          PK1, V3,
          PK2, V4
      );

      map.putAll(replacements);

      // Check that the map contains the recently put entries
      for (Map.Entry<HashCode, String> entry : replacements.entrySet()) {
        HashCode key = entry.getKey();
        assertTrue(map.containsKey(key));
        assertThat(map.get(key), equalTo(entry.getValue()));
      }
    });
  }


  @Test
  public void get() {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);

      assertThat(map.get(PK1), equalTo(V1));
    });
  }

  @Test
  public void getRootHash_EmptyMap() {
    runTestWithView(database::createSnapshot, (map) -> {
      assertThat(map.getRootHash(), equalTo(EMPTY_MAP_ROOT_HASH));
    });
  }

  @Test
  public void getRootHash_NonEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);

      HashCode rootHash = map.getRootHash();
      assertThat(rootHash, notNullValue());
      assertThat(rootHash.bits(), equalTo(Hashing.DEFAULT_HASH_SIZE_BITS));
      assertThat(rootHash, not(equalTo(EMPTY_MAP_ROOT_HASH)));
    });
  }

  @Test
  @Ignore
  public void getProof_EmptyMap() {
    runTestWithView(database::createSnapshot,
        (map) -> assertThat(map, provesNoMappingFor(PK1))
    );
  }

  @Test
  @Ignore
  public void getProof_SingletonMapContains() {
    runTestWithView(database::createFork, (map) -> {
      HashCode key = PK1;
      String value = V1;
      map.put(key, value);

      assertThat(map, provesThatContains(key, value));
    });
  }

  @Test
  @Ignore
  public void getProof_SingletonMapDoesNotContain() {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);

      assertThat(map, provesNoMappingFor(PK2));
    });
  }

  @Test
  @Ignore
  public void getProof_FourEntryMap_LastByte_Contains1() {
    runTestWithView(database::createFork, (map) -> {

      Stream<HashCode> proofKeys = Stream.of(
          (byte) 0b0000_0000,
          (byte) 0b0000_0001,
          (byte) 0b1000_0000,
          (byte) 0b1000_0001
      ).map(ProofMapIndexProxyIntegrationTest::createProofKey);

      List<MapEntry<HashCode, String>> entries = createMapEntries(proofKeys);

      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @Ignore
  public void getProof_FourEntryMap_LastByte_Contains2() {
    runTestWithView(database::createFork, (map) -> {
      Stream<HashCode> proofKeys = Stream.of(
          (byte) 0b00,
          (byte) 0b01,
          (byte) 0b10,
          (byte) 0b11
      ).map(ProofMapIndexProxyIntegrationTest::createProofKey);

      List<MapEntry<HashCode, String>> entries = createMapEntries(proofKeys);

      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @Ignore
  public void getProof_FourEntryMap_FirstByte_Contains() {
    runTestWithView(database::createFork, (map) -> {
      byte[] key1 = createRawProofKey();
      byte[] key2 = createRawProofKey();
      key2[0] = (byte) 0b0000_0001;
      byte[] key3 = createRawProofKey();
      key3[0] = (byte) 0b1000_0000;
      byte[] key4 = createRawProofKey();
      key4[0] = (byte) 0b1000_0001;

      List<MapEntry<HashCode, String>> entries = createMapEntries(
          Stream.of(key1, key2, key3, key4)
              .map(HashCode::fromBytes)
      );

      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @Ignore
  public void getProof_FourEntryMap_FirstAndLastByte_Contains() {
    runTestWithView(database::createFork, (map) -> {
      byte[] key1 = createRawProofKey();  // 000…0
      byte[] key2 = createRawProofKey();  // 100…0
      key2[0] = (byte) 0x01;
      byte[] key3 = createRawProofKey((byte) 0x80);  // 000…01
      byte[] key4 = createRawProofKey((byte) 0x80);  // 100…01
      key4[0] = (byte) 0x01;

      List<MapEntry<HashCode, String>> entries = createMapEntries(
          Stream.of(key1, key2, key3, key4)
              .map(HashCode::fromBytes)
      );

      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @Ignore
  public void getProof_MultiEntryMapContains() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createSortedMapEntries();
      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
    });
  }

  @SuppressWarnings("unused")
  private void printProof(ProofMapIndexProxy<HashCode, String> map, HashCode key) {
    MapProof proof = map.getProof(key);
    System.out.println("\nProof for key: " + key);
    MapProofTreePrinter printer = new MapProofTreePrinter();
    proof.accept(printer);
  }

  @Test
  @Ignore
  public void getProof_MultiEntryMapDoesNotContain() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createSortedMapEntries();
      putAll(map, entries);

      byte[] allOnes = new byte[PROOF_MAP_KEY_SIZE];
      Arrays.fill(allOnes, UnsignedBytes.checkedCast(0xFF));

      List<HashCode> otherKeys = ImmutableList.of(
          HashCode.fromBytes(allOnes),  // [11…1]
          createProofKey("PK1001"),
          createProofKey("PK1002"),
          createProofKey("PK100500")
      );

      for (HashCode key : otherKeys) {
        assertThat(map, provesNoMappingFor(key));
      }
    });
  }

  @Test
  // Takes quite a lot of time (validating 257 proofs),
  // but it's an integration test, isn't it? :-)
  //
  // Consider adding a similar test for left-leaning MPT
  @Ignore
  public void getProof_MapContainsRightLeaningMaxHeightMpt() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createEntriesForRightLeaningMpt();
      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatContains(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  public void remove() {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, V1);
      map.remove(PK1);

      assertNull(map.get(PK1));
      assertFalse(map.containsKey(PK1));
    });
  }

  @Test
  public void removeFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(UnsupportedOperationException.class);
      map.remove(PK1);
    });
  }

  @Test
  public void removeFailsIfInvalidKey() {
    runTestWithView(database::createFork, (map) -> {
      expectedException.expect(IllegalArgumentException.class);
      map.remove(INVALID_PROOF_KEY);
    });
  }

  @Test
  public void keysTest() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createSortedMapEntries();

      putAll(map, entries);

      Iterator<HashCode> keysIterator = map.keys();
      List<HashCode> keysFromIter = ImmutableList.copyOf(keysIterator);
      List<HashCode> keysInMap = MapEntries.extractKeys(entries);

      // Keys must appear in a lexicographical order.
      assertThat(keysFromIter, equalTo(keysInMap));
    });
  }

  @Test
  public void valuesTest() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createSortedMapEntries();

      putAll(map, entries);

      Iterator<String> valuesIterator = map.values();
      List<String> valuesFromIter = ImmutableList.copyOf(valuesIterator);
      List<String> valuesInMap = MapEntries.extractValues(entries);

      // Values must appear in a lexicographical order of keys.
      assertThat(valuesFromIter, equalTo(valuesInMap));
    });
  }

  @Test
  public void entriesTest() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createSortedMapEntries();

      putAll(map, entries);

      Iterator<MapEntry<HashCode, String>> entriesIterator = map.entries();
      List<MapEntry> entriesFromIter = ImmutableList.copyOf(entriesIterator);
      // Entries must appear in a lexicographical order of keys.
      assertThat(entriesFromIter, equalTo(entries));
    });
  }

  @Test
  public void clearEmptyHasNoEffect() {
    runTestWithView(database::createFork, ProofMapIndexProxy::clear);
  }

  @Test
  public void clearNonEmptyRemovesAllValues() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createSortedMapEntries();

      putAll(map, entries);

      map.clear();

      for (MapEntry<HashCode, ?> e : entries) {
        assertFalse(map.containsKey(e.getKey()));
      }
    });
  }

  @Test
  public void clearFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (map) -> {
      expectedException.expect(UnsupportedOperationException.class);
      map.clear();
    });
  }

  /**
   * A simple integration test that ensures that:
   *   - ProofMap constructor preserves the index type and
   *   - Map constructor checks it, preventing illegal access to ProofMap internals.
   */
  @Test
  public void constructorShallPreserveTypeInformation() {
    runTestWithView(database::createFork, (view, map) -> {
      map.put(PK1, "v1");

      expectedException.expectMessage(
          "Attempt to access index '" + MAP_NAME
              + "' of type Map, while said index was initially created with type ProofMap");
      expectedException.expect(RuntimeException.class);
      // Create a regular map with the same name as the proof map above.
      MapIndexProxy<HashCode, String> regularMap = MapIndexProxy.newInstance(MAP_NAME, view,
          StandardSerializers.hash(), StandardSerializers.string());
    });
  }

  @Test
  public void isEmptyShouldReturnTrueForEmptyMap() {
    runTestWithView(database::createSnapshot, (map) -> assertTrue(map.isEmpty()));
  }

  @Test
  public void isEmptyShouldReturnFalseForNonEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      map.put(PK1, "v1");

      assertFalse(map.isEmpty());
    });
  }

  /**
   * Create a proof key of length 32 with the specified suffix.
   *
   * @param suffix a key suffix. Must be shorter than or equal to 32 bytes in UTF-8.
   * @return a key, starting with zeroes and followed by the specified suffix encoded in UTF-8
   */
  private static HashCode createProofKey(String suffix) {
    byte[] suffixBytes = bytes(suffix);
    return createProofKey(suffixBytes);
  }

  private static HashCode createProofKey(byte... suffixBytes) {
    byte[] proofKey = createRawProofKey(suffixBytes);
    return HashCode.fromBytes(proofKey);
  }

  private static byte[] createRawProofKey(byte... suffixBytes) {
    checkArgument(suffixBytes.length <= PROOF_MAP_KEY_SIZE);
    byte[] proofKey = new byte[PROOF_MAP_KEY_SIZE];
    System.arraycopy(suffixBytes, 0, proofKey, PROOF_MAP_KEY_SIZE - suffixBytes.length,
        suffixBytes.length);
    return checkProofKey(proofKey);
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      Consumer<ProofMapIndexProxy<HashCode, String>> mapTest) {
    runTestWithView(viewFactory, (ignoredView, map) -> mapTest.accept(map));
  }

  private static void runTestWithView(
      Function<Cleaner, View> viewFactory,
      BiConsumer<View, ProofMapIndexProxy<HashCode, String>> mapTest) {
    try (Cleaner cleaner = new Cleaner()) {
      View view = viewFactory.apply(cleaner);
      ProofMapIndexProxy<HashCode, String> map = createProofMap(MAP_NAME, view);

      mapTest.accept(view, map);
    } catch (CloseFailuresException e) {
      throw new AssertionError("Unexpected exception", e);
    }
  }

  @Override
  ProofMapIndexProxy<HashCode, String> create(String name, View view) {
    return createProofMap(name, view);
  }

  @Override
  Object getAnyElement(ProofMapIndexProxy<HashCode, String> index) {
    return index.get(PK1);
  }

  private static ProofMapIndexProxy<HashCode, String> createProofMap(String name, View view) {
    return ProofMapIndexProxy.newInstance(name, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  /**
   * Creates `numOfEntries` map entries, sorted by key:
   * [(00…0PK1, V1), (00…0PK2, V2), … (00…0PKi, Vi)].
   */
  private List<MapEntry<HashCode, String>> createSortedMapEntries() {
    // Use PROOF_KEYS which are already sorted.
    return createMapEntries(PROOF_KEYS.stream());
  }

  /**
   * Creates map entries with the given keys. Uses values
   * from {@linkplain TestStorageItems#values} in a round-robin fashion.
   */
  private List<MapEntry<HashCode, String>> createMapEntries(Stream<HashCode> proofKeys) {
    Stream<HashCode> keys = proofKeys.distinct();
    Stream<String> roundRobinValues = IntStream.range(0, Integer.MAX_VALUE)
        .mapToObj(i -> values.get(i % values.size()));
    return Streams.zip(keys, roundRobinValues, MapEntry::from)
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
  private static List<MapEntry<HashCode, String>> createEntriesForRightLeaningMpt() {
    int numKeyBits = Byte.SIZE * PROOF_MAP_KEY_SIZE;
    BitSet keyBits = new BitSet(numKeyBits);
    int numEntries = numKeyBits + 1;
    List<MapEntry<HashCode, String>> entries = new ArrayList<>(numEntries);
    entries.add(MapEntry.from(HashCode.fromBytes(new byte[PROOF_MAP_KEY_SIZE]), V1));

    for (int i = 0; i < numKeyBits; i++) {
      keyBits.set(i);
      byte[] key = createPrefixed(keyBits.toByteArray(), PROOF_MAP_KEY_SIZE);
      String value = values.get(i % values.size());
      entries.add(MapEntry.from(HashCode.fromBytes(key), value));
      keyBits.clear(i);
      assert keyBits.length() == 0;
    }

    return entries;
  }
}
