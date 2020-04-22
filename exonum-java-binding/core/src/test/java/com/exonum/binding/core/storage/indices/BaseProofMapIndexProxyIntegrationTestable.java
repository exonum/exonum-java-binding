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

import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static com.exonum.binding.core.storage.indices.MapEntries.putAll;
import static com.exonum.binding.core.storage.indices.MapTestEntry.absentEntry;
import static com.exonum.binding.core.storage.indices.MapTestEntry.presentEntry;
import static com.exonum.binding.core.storage.indices.ProofMapContainsMatcher.provesThatAbsent;
import static com.exonum.binding.core.storage.indices.ProofMapContainsMatcher.provesThatCorrect;
import static com.exonum.binding.core.storage.indices.ProofMapContainsMatcher.provesThatPresent;
import static com.exonum.binding.core.storage.indices.StoragePreconditions.PROOF_MAP_KEY_SIZE;
import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkProofKey;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V3;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V4;
import static com.exonum.binding.core.storage.indices.TestStorageItems.values;
import static com.exonum.binding.test.Bytes.bytes;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.messages.proof.MapProofOuterClass;
import com.exonum.messages.proof.MapProofOuterClass.OptionalEntry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.primitives.UnsignedBytes;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

abstract class BaseProofMapIndexProxyIntegrationTestable
    extends BaseIndexProxyTestable<ProofMapIndexProxy<HashCode, String>> {

  private static final String MAP_NAME = "test_proof_map";

  private final HashCode key1 = getTestKeys().get(0);
  private final HashCode key2 = getTestKeys().get(1);
  private final HashCode key3 = getTestKeys().get(2);

  private static final HashCode EMPTY_MAP_INDEX_HASH = HashCode.fromString(
      "7324b5c72b51bb5d4c180f1109cfd347b60473882145841c39f3e584576296f9");

  abstract List<HashCode> getTestKeys();

  @Test
  void containsKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);
      assertTrue(map.containsKey(key1));
    });
  }

  @Test
  void doesNotContainAbsentKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);
      assertFalse(map.containsKey(key2));
    });
  }

  @Test
  void emptyMapDoesNotContainAbsentKey() {
    runTestWithView(database::createSnapshot, (map) -> assertFalse(map.containsKey(key2)));
  }

  @Test
  void containsThrowsIfNullKey() {
    runTestWithView(database::createSnapshot,
        (map) -> assertThrows(NullPointerException.class, () -> map.containsKey(null)));
  }

  @Test
  void putFailsIfSnapshot() {
    runTestWithView(database::createSnapshot,
        (map) -> assertThrows(UnsupportedOperationException.class, () -> map.put(key1, V1)));
  }

  @Test
  void putAllInEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      ImmutableMap<HashCode, String> source = ImmutableMap.of(
          key1, V1,
          key2, V2
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
  void putAllOverwritingEntries() {
    runTestWithView(database::createFork, (map) -> {
      map.putAll(ImmutableMap.of(
          key1, V1,
          key2, V2
      ));

      ImmutableMap<HashCode, String> replacements = ImmutableMap.of(
          key1, V3,
          key2, V4
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
  void get() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      assertThat(map.get(key1), equalTo(V1));
    });
  }

  @Test
  void getIndexHash_EmptyMap() {
    runTestWithView(database::createSnapshot,
        (map) -> assertThat(map.getIndexHash(), equalTo(EMPTY_MAP_INDEX_HASH)));
  }

  @Test
  void getIndexHash_NonEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      HashCode indexHash = map.getIndexHash();
      assertThat(indexHash, notNullValue());
      assertThat(indexHash.bits(), equalTo(Hashing.DEFAULT_HASH_SIZE_BITS));
      assertThat(indexHash, not(equalTo(EMPTY_MAP_INDEX_HASH)));
    });
  }

  @Test
  @DisabledProofTest
  void verifyProof_EmptyMapDoesNotContainSingleKey() {
    runTestWithView(database::createSnapshot,
        (map) -> assertThat(map, provesThatAbsent(key1))
    );
  }

  @Test
  @DisabledProofTest
  void verifyProof_SingletonMapContains() {
    runTestWithView(database::createFork, (map) -> {
      HashCode key = key1;
      String value = V1;
      map.put(key, value);

      assertThat(map, provesThatPresent(key, value));
    });
  }

  @Test
  @DisabledProofTest
  void verifyProof_SingletonMapDoesNotContain() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      assertThat(map, provesThatAbsent(key2));
    });
  }

  @Test
  @DisabledProofTest
  void verifyProof_MultiEntryMapContains() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createMapEntries();
      putAll(map, entries);

      for (MapEntry<HashCode, String> e : entries) {
        assertThat(map, provesThatPresent(e.getKey(), e.getValue()));
      }
    });
  }

  @Test
  @DisabledProofTest
  void verifyMultiProof_MultiEntryMapContains() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createMapEntries();
      putAll(map, entries);

      assertThat(map, provesThatPresent(entries));
    });
  }

  @Test
  @DisabledProofTest
  void verifyProof_MultiEntryMapDoesNotContain() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createMapEntries();
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
        assertThat(map, provesThatAbsent(key));
      }
    });
  }

  @Test
  @DisabledProofTest
  void verifyMultiProof_EmptyMapDoesNotContainSeveralKeys() {
    runTestWithView(database::createSnapshot, (map) ->
        assertThat(map, provesThatAbsent(key1, key2)));
  }

  @Test
  @DisabledProofTest
  void verifyMultiProof_SingletonMapDoesNotContainSeveralKeys() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      assertThat(map, provesThatAbsent(key2, key3));
    });
  }

  @Test
  @DisabledProofTest
  void verifyMultiProof_SingletonMapBothContainsAndDoesNot() {
    runTestWithView(database::createFork, (map) -> {
      ImmutableMap<HashCode, String> source = ImmutableMap.of(
          key1, V1
      );

      map.putAll(source);

      assertThat(map, provesThatCorrect(presentEntry(key1, V1), absentEntry(key2)));
    });
  }

  @Test
  @DisabledProofTest
  void verifyMultiProof_TwoElementMapContains() {
    runTestWithView(database::createFork, (map) -> {
      ImmutableMap<HashCode, String> source = ImmutableMap.of(
          key1, V1,
          key2, V2
      );

      map.putAll(source);

      assertThat(map, provesThatCorrect(presentEntry(key1, V1), presentEntry(key2, V2)));
    });
  }

  @Test
  void remove() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);
      map.remove(key1);

      assertNull(map.get(key1));
      assertFalse(map.containsKey(key1));
    });
  }

  @Test
  void removeFailsIfSnapshot() {
    runTestWithView(database::createSnapshot,
        (map) -> assertThrows(UnsupportedOperationException.class, () -> map.remove(key1)));
  }

  @Test
  void clearEmptyHasNoEffect() {
    runTestWithView(database::createFork, map -> assertThatCode(map::clear)
        .doesNotThrowAnyException());
  }

  @Test
  void clearNonEmptyRemovesAllValues() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createMapEntries();

      putAll(map, entries);

      map.clear();

      for (MapEntry<HashCode, ?> e : entries) {
        assertFalse(map.containsKey(e.getKey()));
      }
    });
  }

  @Test
  void clearFailsIfSnapshot() {
    runTestWithView(database::createSnapshot, (map) -> {
      assertThrows(UnsupportedOperationException.class, () -> map.clear());
    });
  }

  @Test
  void isEmptyShouldReturnTrueForEmptyMap() {
    runTestWithView(database::createSnapshot, (map) -> assertTrue(map.isEmpty()));
  }

  @Test
  void isEmptyShouldReturnFalseForNonEmptyMap() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      assertFalse(map.isEmpty());
    });
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
  void getProofFromSingleKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      MapProof proof = map.getProof(key1);
      MapProofOuterClass.MapProof asMessage = proof.getAsMessage();

      assertThat(asMessage.getEntriesList()).containsExactly(presentOptEntry(key1, V1));
    });
  }

  @Test
  void getProofFromSingleMissingKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      MapProof proof = map.getProof(key2);
      MapProofOuterClass.MapProof asMessage = proof.getAsMessage();

      assertThat(asMessage.getEntriesList()).containsExactly(absentOptEntry(key2));
    });
  }

  @Test
  void getProofFromVarargsPresentKeys() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);
      map.put(key2, V2);

      MapProof proof = map.getProof(key1, key2);

      MapProofOuterClass.MapProof asMessage = proof.getAsMessage();
      assertThat(asMessage.getEntriesList()).containsExactlyInAnyOrder(
          presentOptEntry(key1, V1), presentOptEntry(key2, V2));
    });
  }

  @Test
  void getProofFromVarargsPresentAndMissingKeys() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      MapProof proof = map.getProof(key1, key2);

      MapProofOuterClass.MapProof asMessage = proof.getAsMessage();
      assertThat(asMessage.getEntriesList()).containsExactlyInAnyOrder(
          presentOptEntry(key1, V1), absentOptEntry(key2));
    });
  }

  @Test
  void getProofFromCollection() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      MapProof proof = map.getProof(singletonList(key1));

      MapProofOuterClass.MapProof asMessage = proof.getAsMessage();
      assertThat(asMessage.getEntriesList()).containsExactly(
          presentOptEntry(key1, V1));
    });
  }

  @Test
  void getProofFromEmptyCollection() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
          () -> map.getProof(emptyList()));
      assertThat(thrown.getLocalizedMessage(),
          containsString("Keys collection should not be empty"));
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

  static HashCode createProofKey(byte... suffixBytes) {
    byte[] proofKey = createRawProofKey(suffixBytes);
    return HashCode.fromBytes(proofKey);
  }

  static byte[] createRawProofKey(byte... suffixBytes) {
    checkArgument(suffixBytes.length <= PROOF_MAP_KEY_SIZE);
    byte[] proofKey = new byte[PROOF_MAP_KEY_SIZE];
    System.arraycopy(suffixBytes, 0, proofKey, PROOF_MAP_KEY_SIZE - suffixBytes.length,
        suffixBytes.length);
    return checkProofKey(proofKey);
  }

  void runTestWithView(Function<Cleaner, Access> accessFactory,
      Consumer<ProofMapIndexProxy<HashCode, String>> mapTest) {
    runTestWithView(accessFactory, (ignoredView, map) -> mapTest.accept(map));
  }

  private void runTestWithView(
      Function<Cleaner, Access> accessFactory,
      BiConsumer<Access, ProofMapIndexProxy<HashCode, String>> mapTest) {
    try (Cleaner cleaner = new Cleaner()) {
      Access access = accessFactory.apply(cleaner);
      ProofMapIndexProxy<HashCode, String> map = this.create(MAP_NAME, access);

      mapTest.accept(access, map);
    } catch (CloseFailuresException e) {
      throw new AssertionError("Unexpected exception", e);
    }
  }

  @Override
  StorageIndex createOfOtherType(String name, Access access) {
    return access.getList(IndexAddress.valueOf(name), string());
  }

  @Override
  Object getAnyElement(ProofMapIndexProxy<HashCode, String> index) {
    return index.get(key1);
  }

  @Override
  void update(ProofMapIndexProxy<HashCode, String> index) {
    index.put(key1, V1);
  }

  /**
   * Creates `numOfEntries` map entries, sorted by key in lexicographical order:
   * [(key0, V1), (key2, V2), … (key_i, Vi)].
   */
  private List<MapEntry<HashCode, String>> createSortedMapEntries() {
    Stream<HashCode> sortedKeys = getTestKeys().stream()
        .sorted(comparing(HashCode::asBytes, UnsignedBytes.lexicographicalComparator()));
    return createMapEntries(sortedKeys);
  }

  private List<MapEntry<HashCode, String>> createMapEntries() {
    return createMapEntries(getTestKeys().stream());
  }

  /**
   * Creates map entries with the given keys. Uses values
   * from {@linkplain TestStorageItems#values} in a round-robin fashion.
   */
  List<MapEntry<HashCode, String>> createMapEntries(Stream<HashCode> proofKeys) {
    Stream<HashCode> keys = proofKeys.distinct();
    Stream<String> roundRobinValues = IntStream.range(0, Integer.MAX_VALUE)
        .mapToObj(i -> values.get(i % values.size()));
    return Streams.zip(keys, roundRobinValues, MapEntry::valueOf)
        .collect(toList());
  }

  private static OptionalEntry presentOptEntry(HashCode key, String value) {
    Serializer<String> stringSerializer = StandardSerializers.string();
    return optEntryForKey(key)
        .setValue(ByteString.copyFrom(stringSerializer.toBytes(value)))
        .build();
  }

  private static OptionalEntry absentOptEntry(HashCode key) {
    return optEntryForKey(key)
        .setNoValue(Empty.getDefaultInstance())
        .build();
  }

  private static OptionalEntry.Builder optEntryForKey(HashCode key) {
    Serializer<HashCode> hashSerializer = StandardSerializers.hash();
    return OptionalEntry.newBuilder()
        .setKey(ByteString.copyFrom(hashSerializer.toBytes(key)));
  }
}
