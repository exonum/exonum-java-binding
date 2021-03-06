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

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BITS;
import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static com.exonum.binding.core.storage.indices.ProofListContainsMatcher.provesAbsence;
import static com.exonum.binding.core.storage.indices.ProofListContainsMatcher.provesThatContains;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V3;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.messages.proof.ListProofOuterClass;
import com.exonum.messages.proof.ListProofOuterClass.ListProofEntry;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Inherits base tests of ListIndex interface methods and also contains tests
 * of ProofListIndexProxy methods that are not present in {@link ListIndex} interface.
 */
class ProofListIndexProxyIntegrationTest extends BaseListIndexIntegrationTestable {

  private static final HashCode EMPTY_LIST_INDEX_HASH =
      HashCode.fromString("c6c0aa07f27493d2f2e5cff56c890a353a20086d6c25ec825128e12ae752b2d9");

  private static final String LIST_NAME = "test_proof_list";

  @Override
  ProofListIndexProxy<String> create(String name, Access access) {
    return access.getProofList(IndexAddress.valueOf(name), string());
  }

  @Override
  ProofListIndexProxy<String> createInGroup(String groupName, byte[] idInGroup, Access access) {
    return access.getProofList(IndexAddress.valueOf(groupName, idInGroup), string());
  }

  @Override
  StorageIndex createOfOtherType(String name, Access access) {
    return access.getList(IndexAddress.valueOf(name), string());
  }

  @Override
  Object getAnyElement(AbstractListIndexProxy<String> index) {
    return index.get(0L);
  }

  @Override
  void update(AbstractListIndexProxy<String> index) {
    index.add(V1);
  }

  @Test
  void getIndexHashEmptyList() {
    runTestWithView(database::createSnapshot, (list) -> {
      assertThat(list.getIndexHash(), equalTo(EMPTY_LIST_INDEX_HASH));
    });
  }

  @Test
  void getIndexHashSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      HashCode indexHash = list.getIndexHash();
      assertThat(indexHash.bits(), equalTo(DEFAULT_HASH_SIZE_BITS));
      assertThat(indexHash, not(equalTo(EMPTY_LIST_INDEX_HASH)));
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2})
  void getProofThreeElementList(int index) {
    runTestWithView(database::createFork, (list) -> {
      List<String> elements = asList(V1, V2, V3);
      list.addAll(elements);

      ListProof proof = list.getProof(index);

      ListProofOuterClass.ListProof asMessage = proof.getAsMessage();
      assertThat(asMessage.getLength()).isEqualTo(3L);
      ListProofEntry expectedEntry = listProofEntry(index, elements.get(index));
      assertThat(asMessage.getEntriesList()).containsExactly(expectedEntry);
    });
  }

  @ParameterizedTest
  @ValueSource(longs = {3, 4, Long.MAX_VALUE})
  void getProofThreeElementListOutOfRange(long index) {
    runTestWithView(database::createFork, (list) -> {
      List<String> elements = asList(V1, V2, V3);
      list.addAll(elements);

      ListProof proof = list.getProof(index);

      ListProofOuterClass.ListProof asMessage = proof.getAsMessage();
      assertThat(asMessage.getLength()).isEqualTo(3L);
      assertThat(asMessage.getEntriesList()).isEmpty();
    });
  }

  @Test
  void getRangeProofThreeElementListFullRange() {
    runTestWithView(database::createFork, (list) -> {
      List<String> elements = asList(V1, V2, V3);
      list.addAll(elements);

      ListProof proof = list.getRangeProof(0, 3);

      ListProofOuterClass.ListProof asMessage = proof.getAsMessage();
      assertThat(asMessage.getLength()).isEqualTo(3L);
      assertThat(asMessage.getEntriesList()).containsExactlyInAnyOrder(
          listProofEntry(0, V1), listProofEntry(1, V2), listProofEntry(2, V3));
    });
  }

  @Test
  void getRangeProofThreeElementListHalfInRange() {
    runTestWithView(database::createFork, (list) -> {
      List<String> elements = asList(V1, V2, V3);
      list.addAll(elements);

      ListProof proof = list.getRangeProof(2, 4);

      ListProofOuterClass.ListProof asMessage = proof.getAsMessage();
      assertThat(asMessage.getLength()).isEqualTo(3L);
      assertThat(asMessage.getEntriesList()).containsExactly(listProofEntry(2, V3));
    });
  }

  private static ListProofEntry listProofEntry(long index, String element) {
    Serializer<String> serializer = string();
    return ListProofEntry.newBuilder()
        .setIndex(index)
        .setValue(ByteString.copyFrom(serializer.toBytes(element)))
        .build();
  }

  @Test
  @DisabledProofTest
  void verifyProofSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, V1));
    });
  }

  @Test
  @DisabledProofTest
  void verifyProofOfAbsenceEmptyList() {
    runTestWithView(database::createFork, (list) -> {
      assertThat(list, provesAbsence(0));
    });
  }

  @Test
  @DisabledProofTest
  void verifyProofOfAbsenceSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesAbsence(1));
    });
  }

  @Test
  @DisabledProofTest
  void verifyRangeProofOfAbsenceEmptyList() {
    runTestWithView(database::createFork, (list) -> {
      assertThat(list, provesAbsence(0, 1));
    });
  }

  @Test
  @DisabledProofTest
  void verifyRangeProofOfAbsenceSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesAbsence(0, 2));
    });
  }

  @Test
  @DisabledProofTest
  void verifyRangeProofSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, singletonList(V1)));
    });
  }

  @ParameterizedTest
  @DisabledProofTest
  @ValueSource(ints = {2, 3, 4, 5, 7, 8, 9})
  void verifyProofMultipleItemList(int size) {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values.subList(0, size);

      list.addAll(values);

      for (int i = 0; i < values.size(); i++) {
        assertThat(list, provesThatContains(i, values.get(i)));
      }
    });
  }

  @Test
  @DisabledProofTest
  void verifyRangeProofMultipleItemList_FullRange() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      assertThat(list, provesThatContains(0, values));
    });
  }

  @Test
  @DisabledProofTest
  void verifyRangeProofMultipleItemList_1stHalf() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = 0;
      int to = values.size() / 2;
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  @Test
  @DisabledProofTest
  void verifyRangeProofMultipleItemList_2ndHalf() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = values.size() / 2;
      int to = values.size();
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  @DisabledProofTest
  @Disabled("ECR-3673: empty ranges are not supported with the current tree format; "
      + "need a flat one")
  void verifyRangeProofMultipleItemList_EmptyRange(int size) {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values.subList(0, size);

      list.addAll(values);

      assertThat(list, provesThatContains(0, emptyList()));
    });
  }

  private static void runTestWithView(Function<Cleaner, Access> viewFactory,
                                      Consumer<ProofListIndexProxy<String>> listTest) {
    runTestWithView(viewFactory, (ignoredView, list) -> listTest.accept(list));
  }

  private static void runTestWithView(Function<Cleaner, Access> viewFactory,
                                      BiConsumer<Access, ProofListIndexProxy<String>> listTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        LIST_NAME,
        ((address, access, serializer) -> access.getProofList(address, serializer)),
        listTest
    );
  }
}
