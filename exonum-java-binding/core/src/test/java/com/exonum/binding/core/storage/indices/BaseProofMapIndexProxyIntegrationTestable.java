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

import static com.exonum.binding.core.storage.indices.CheckedMapProofMatcher.isValid;
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
import static java.util.Collections.singletonList;
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
import com.exonum.binding.common.proofs.map.CheckedMapProof;
import com.exonum.binding.common.proofs.map.UncheckedMapProof;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.View;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.primitives.UnsignedBytes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  void getProof_EmptyMapDoesNotContainSingleKey() {
    runTestWithView(database::createSnapshot,
        (map) -> assertThat(map, provesThatAbsent(key1))
    );
  }

  @Test
  @DisabledProofTest
  void getProof_SingletonMapContains() {
    runTestWithView(database::createFork, (map) -> {
      HashCode key = key1;
      String value = V1;
      map.put(key, value);

      assertThat(map, provesThatPresent(key, value));
    });
  }

  @Test
  @DisabledProofTest
  void getProof_SingletonMapDoesNotContain() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      assertThat(map, provesThatAbsent(key2));
    });
  }

  @Test
  @DisabledProofTest
  void getProof_MultiEntryMapContains() {
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
  void getMultiProof_MultiEntryMapContains() {
    runTestWithView(database::createFork, (map) -> {
      List<MapEntry<HashCode, String>> entries = createMapEntries();
      putAll(map, entries);

      assertThat(map, provesThatPresent(entries));
    });
  }

  @Test
  @DisabledProofTest
  void getProof_MultiEntryMapDoesNotContain() {
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
  void getMultiProof_EmptyMapDoesNotContainSeveralKeys() {
    runTestWithView(database::createSnapshot, (map) ->
        assertThat(map, provesThatAbsent(key1, key2)));
  }

  @Test
  @DisabledProofTest
  void getMultiProof_SingletonMapDoesNotContainSeveralKeys() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      assertThat(map, provesThatAbsent(key2, key3));
    });
  }

  @Test
  @DisabledProofTest
  void getMultiProof_SingletonMapBothContainsAndDoesNot() {
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
  void getMultiProof_TwoElementMapContains() {
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
    runTestWithView(database::createFork, ProofMapIndexProxy::clear);
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
  @DisabledProofTest
  void getProofFromSingleKey() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      UncheckedMapProof proof = map.getProof(key1);
      CheckedMapProof checkedProof = proof.check();

      assertThat(checkedProof, isValid(singletonList(presentEntry(key1, V1))));
    });
  }

  @Test
  @DisabledProofTest
  void getProofFromVarargs() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);
      map.put(key2, V2);

      UncheckedMapProof proof = map.getProof(key1, key2);
      CheckedMapProof checkedProof = proof.check();

      assertThat(
          checkedProof, isValid(Arrays.asList(presentEntry(key1, V1), presentEntry(key2, V2))));
    });
  }

  @Test
  @DisabledProofTest
  void getProofFromEmptyCollection() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
        UncheckedMapProof proof = map.getProof(Collections.emptyList());
      });
      assertThat(thrown.getLocalizedMessage(),
          containsString("Keys collection should not be empty"));
    });
  }

  @Test
  @DisabledProofTest
  void getProofFromCollection() {
    runTestWithView(database::createFork, (map) -> {
      map.put(key1, V1);

      UncheckedMapProof proof = map.getProof(singletonList(key1));
      CheckedMapProof checkedProof = proof.check();

      assertThat(checkedProof, isValid(singletonList(presentEntry(key1, V1))));
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

  void runTestWithView(Function<Cleaner, View> viewFactory,
      Consumer<ProofMapIndexProxy<HashCode, String>> mapTest) {
    runTestWithView(viewFactory, (ignoredView, map) -> mapTest.accept(map));
  }

  private void runTestWithView(
      Function<Cleaner, View> viewFactory,
      BiConsumer<View, ProofMapIndexProxy<HashCode, String>> mapTest) {
    try (Cleaner cleaner = new Cleaner()) {
      View view = viewFactory.apply(cleaner);
      ProofMapIndexProxy<HashCode, String> map = this.create(MAP_NAME, view);

      mapTest.accept(view, map);
    } catch (CloseFailuresException e) {
      throw new AssertionError("Unexpected exception", e);
    }
  }

  @Override
  StorageIndex createOfOtherType(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  Object getAnyElement(ProofMapIndexProxy<HashCode, String> index) {
    return index.get(key1);
  }

  @Override
  void update(ProofMapIndexProxy<HashCode, String> index) {
    index.put(key1, V1);
  }

  List<MapEntry<HashCode, String>> createMapEntries() {
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
        .collect(Collectors.toList());
  }
}
