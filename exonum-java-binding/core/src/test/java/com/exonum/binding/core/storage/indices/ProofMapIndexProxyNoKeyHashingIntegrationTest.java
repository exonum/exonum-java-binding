/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.core.storage.indices.MapEntries.putAll;
import static com.exonum.binding.core.storage.indices.ProofMapContainsMatcher.provesThatAbsent;
import static com.exonum.binding.core.storage.indices.ProofMapContainsMatcher.provesThatPresent;
import static com.exonum.binding.core.storage.indices.StoragePreconditions.PROOF_MAP_KEY_SIZE;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.values;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.CiOnly;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedBytes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProofMapIndexProxyNoKeyHashingIntegrationTest
    extends BaseProofMapIndexProxyIntegrationTestable {

  static final List<HashCode> SORTED_TEST_KEYS = Stream.of(
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
      .map(BaseProofMapIndexProxyIntegrationTestable::createRawProofKey)
      .sorted(UnsignedBytes.lexicographicalComparator())
      .map(HashCode::fromBytes)
      .collect(Collectors.toList());

  private static final HashCode INVALID_PROOF_KEY = HashCode.fromString("1234");

  @Override
  List<HashCode> getTestKeys() {
    return SORTED_TEST_KEYS;
  }

  @Override
  ProofMapIndexProxy<HashCode, String> create(String name, View view) {
    return createProofMap(name, view);
  }

  @Override
  ProofMapIndexProxy<HashCode, String> createInGroup(String groupName, byte[] idInGroup,
                                                     View view) {
    return ProofMapIndexProxy.newInGroupUnsafeNoKeyHashing(groupName, idInGroup, view,
        StandardSerializers.hash(), StandardSerializers.string());
  }

  private static ProofMapIndexProxy<HashCode, String> createProofMap(String name, View view) {
    return ProofMapIndexProxy.newInstanceNoKeyHashing(name, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  @Test
  void containsThrowsIfInvalidKey() {
    runTestWithView(database::createSnapshot, (map) -> assertThrows(IllegalArgumentException.class,
        () -> map.containsKey(INVALID_PROOF_KEY)));
  }

  @Test
  void putFailsIfInvalidKey() {
    runTestWithView(database::createFork, (map) -> assertThrows(IllegalArgumentException.class,
        () -> map.put(INVALID_PROOF_KEY, V1)));
  }

  @Test
  void removeFailsIfInvalidKey() {
    runTestWithView(database::createFork,
        (map) -> assertThrows(IllegalArgumentException.class, () -> map.remove(INVALID_PROOF_KEY)));
  }

  @Test
  void keysTest() {
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
  void valuesTest() {
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
  void entriesTest() {
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
  @DisabledProofTest
  void getProof_FourEntryMap_LastByte_Contains1() {
    runTestWithView(database::createFork, (map) -> {

      Stream<HashCode> proofKeys = Stream.of(
          (byte) 0b0000_0000,
          (byte) 0b0000_0001,
          (byte) 0b1000_0000,
          (byte) 0b1000_0001
      ).map(BaseProofMapIndexProxyIntegrationTestable::createProofKey);

      List<MapEntry<HashCode, String>> entries = createMapEntries(proofKeys);

      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatPresent(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @DisabledProofTest
  void getProof_FourEntryMap_LastByte_Contains2() {
    runTestWithView(database::createFork, (map) -> {
      Stream<HashCode> proofKeys = Stream.of(
          (byte) 0b00,
          (byte) 0b01,
          (byte) 0b10,
          (byte) 0b11
      ).map(BaseProofMapIndexProxyIntegrationTestable::createProofKey);

      List<MapEntry<HashCode, String>> entries = createMapEntries(proofKeys);

      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatPresent(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @DisabledProofTest
  void getProof_FourEntryMap_FirstByte_Contains() {
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
        assertThat(map, provesThatPresent(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @DisabledProofTest
  void getProof_FourEntryMap_FirstAndLastByte_Contains() {
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
        assertThat(map, provesThatPresent(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @DisabledProofTest
  void getMultiProof_FourEntryMap_LastByte_Contains1() {
    runTestWithView(database::createFork, (map) -> {

      Stream<HashCode> proofKeys = Stream.of(
          (byte) 0b0000_0000,
          (byte) 0b0000_0001,
          (byte) 0b1000_0000,
          (byte) 0b1000_0001
      ).map(BaseProofMapIndexProxyIntegrationTestable::createProofKey);

      List<MapEntry<HashCode, String>> entries = createMapEntries(proofKeys);

      putAll(map, entries);

      assertThat(map, provesThatPresent(entries));
    });
  }

  @Test
  @DisabledProofTest
  void getMultiProof_FourEntryMap_LastByte_Contains2() {
    runTestWithView(database::createFork, (map) -> {
      Stream<HashCode> proofKeys = Stream.of(
          (byte) 0b00,
          (byte) 0b01,
          (byte) 0b10,
          (byte) 0b11
      ).map(BaseProofMapIndexProxyIntegrationTestable::createProofKey);

      List<MapEntry<HashCode, String>> entries = createMapEntries(proofKeys);

      putAll(map, entries);

      assertThat(map, provesThatPresent(entries));
    });
  }

  @Test
  @DisabledProofTest
  void getMultiProof_FourEntryMap_FirstByte_Contains() {
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

      assertThat(map, provesThatPresent(entries));
    });
  }

  @Test
  @DisabledProofTest
  void getMultiProof_FourEntryMap_FirstAndLastByte_Contains() {
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

      assertThat(map, provesThatPresent(entries));
    });
  }

  @Test
  @DisabledProofTest
  void getMultiProof_FourEntryMap_DoesNotContain() {
    runTestWithView(database::createFork, (map) -> {
      /*
       This map will have the following structure:
                   <00xxxx>
                   /        \
           <00|00xx>          <00|10xx>
          /         \        /         \
       <0000|01>  <0000|11>  <0010|00>   <0010|10>
      */
      List<MapEntry<HashCode, String>> entries = createMapEntries(
          Stream.of(
              proofKeyFromPrefix("0000|01"),
              proofKeyFromPrefix("0000|11"),
              proofKeyFromPrefix("0010|00"),
              proofKeyFromPrefix("0010|10")
          )
      );

      putAll(map, entries);

      List<HashCode> proofKeys = Arrays.asList(
          // Should be rejected on root level
          proofKeyFromPrefix("01|0000"),
          // Should be rejected on intermediate level
          proofKeyFromPrefix("00|01"),
          proofKeyFromPrefix("00|11"),
          // Should be rejected on leaf level
          proofKeyFromPrefix("0000|00"),
          proofKeyFromPrefix("0000|10"),
          proofKeyFromPrefix("0010|01"),
          proofKeyFromPrefix("0010|11")
      );

      assertThat(map, provesThatAbsent(proofKeys));
    });
  }

  @CiOnly
  @DisabledProofTest
  @Test
  /*
    Takes quite a lot of time (validating 257 proofs), but it's an integration test, isn't it? :-)
    Consider adding a similar test for left-leaning MPT
   */
  void getProof_MapContainsRightLeaningMaxHeightMpt() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createEntriesForRightLeaningMpt();
      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatPresent(e.getKey(), e.getValue()));
      }
    });
  }

  /**
   * Returns a new key with the given prefix.
   *
   * @param prefix a key prefix — from the least significant bit to the most significant,
   *        i.e., "00 01" is 8, "10 00" is 1.
   *        May contain spaces, underscores or bars (e.g., "00 01|01 11" and "11_10"
   *        are valid strings).
   */
  private static HashCode proofKeyFromPrefix(String prefix) {
    prefix = filterBitPrefix(prefix);
    byte[] key = keyFromString(prefix);
    return HashCode.fromBytes(key);
  }

  /**
   * Replaces spaces that may be used to separate groups of binary digits.
   */
  private static String filterBitPrefix(String prefix) {
    String filtered = prefix.replaceAll("[ _|]", "");
    // Check that the string is correct
    assert filtered.matches("[01]*");
    assert filtered.length() <= PROOF_MAP_KEY_SIZE;
    return filtered;
  }

  /**
   * Creates a 32-byte key from the bit prefix.
   */
  private static byte[] keyFromString(String prefix) {
    BitSet keyPrefixBits = new BitSet(prefix.length());
    for (int i = 0; i < prefix.length(); i++) {
      char bit = prefix.charAt(i);
      if (bit == '1') {
        keyPrefixBits.set(i);
      }
    }
    return createPrefixed(keyPrefixBits.toByteArray(), PROOF_MAP_KEY_SIZE);
  }

  /**
   * Creates `numOfEntries` map entries, sorted by key:
   * [(00…0PK1, V1), (00…0PK2, V2), … (00…0PKi, Vi)].
   */
  List<MapEntry<HashCode, String>> createSortedMapEntries() {
    return createMapEntries(SORTED_TEST_KEYS.stream());
  }

  /**
   * Creates 257 entries for a ProofMap that, when added to it, will make the underlying
   * Merkle-Patricia tree of the maximum height (256). Leaf nodes will be at depths
   * ranging from 1 to 256.
   *
   * Bits of 32-byte keys:
   * 00…0000
   * 100…000
   * 0100…00
   * 00100…0
   * …
   * 00…0100
   * 00…0010
   * 00…0001.
   *
   * When all the keys above are added to the ProofMap, the underlying Merkle-Patricia tree
   * has the following structure (only key bits in leaf nodes are shown; the intermediate
   * nodes are shown as 'o' character):
   *
   *                    o — the root node
   *                   / \
   *                  o   100…000 — a leaf node
   *                 / \
   *                o   0100…00
   *               / \
   *              o   00100…0
   *             / \
   *            …   00010…0
   *           /
   *          o — an intermediate node with key prefix 00…0 of size 255 bits.
   *         / \
   *  00…0000  00…0001 — leaf nodes at depth 256 with a common prefix of 255 bits.
   */
  private static List<MapEntry<HashCode, String>> createEntriesForRightLeaningMpt() {
    int numKeyBits = Byte.SIZE * PROOF_MAP_KEY_SIZE;
    BitSet keyBits = new BitSet(numKeyBits);
    int numEntries = numKeyBits + 1;
    List<MapEntry<HashCode, String>> entries = new ArrayList<>(numEntries);
    entries.add(MapEntry.valueOf(HashCode.fromBytes(new byte[PROOF_MAP_KEY_SIZE]), V1));

    for (int i = 0; i < numKeyBits; i++) {
      keyBits.set(i);
      byte[] key = createPrefixed(keyBits.toByteArray(), PROOF_MAP_KEY_SIZE);
      String value = values.get(i % values.size());
      entries.add(MapEntry.valueOf(HashCode.fromBytes(key), value));
      keyBits.clear(i);
      assert keyBits.length() == 0;
    }

    return entries;
  }
}
