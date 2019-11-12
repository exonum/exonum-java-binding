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
import static com.exonum.binding.core.storage.indices.StoragePreconditions.PROOF_MAP_KEY_SIZE;
import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkProofKey;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedBytes;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProofMapIndexProxyNoKeyHashingIntegrationTest
    extends BaseProofMapIndexProxyIntegrationTest {

  static final List<HashCode> PROOF_KEYS = Stream.of(
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
      .map(ProofMapIndexProxyNoKeyHashingIntegrationTest::createRawProofKey)
      .sorted(UnsignedBytes.lexicographicalComparator())
      .map(HashCode::fromBytes)
      .collect(Collectors.toList());

  private static final HashCode INVALID_PROOF_KEY = HashCode.fromString("1234");

  @Override
  List<HashCode> getProofKeys() {
    return PROOF_KEYS;
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

  static byte[] createRawProofKey(byte... suffixBytes) {
    checkArgument(suffixBytes.length <= PROOF_MAP_KEY_SIZE);
    byte[] proofKey = new byte[PROOF_MAP_KEY_SIZE];
    System.arraycopy(suffixBytes, 0, proofKey, PROOF_MAP_KEY_SIZE - suffixBytes.length,
        suffixBytes.length);
    return checkProofKey(proofKey);
  }
}
