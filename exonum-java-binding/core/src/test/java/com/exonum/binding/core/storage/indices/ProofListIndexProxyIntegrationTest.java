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
import static com.exonum.binding.core.storage.indices.ProofListContainsMatcher.provesAbsence;
import static com.exonum.binding.core.storage.indices.ProofListContainsMatcher.provesThatContains;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.database.View;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Contains tests of ProofListIndexProxy methods
 * that are not present in {@link ListIndex} interface.
 */
class ProofListIndexProxyIntegrationTest extends BaseListIndexIntegrationTestable {

  private static final HashCode EMPTY_LIST_INDEX_HASH =
      HashCode.fromString("c6c0aa07f27493d2f2e5cff56c890a353a20086d6c25ec825128e12ae752b2d9");

  private static final String LIST_NAME = "test_proof_list";

  @Override
  ProofListIndexProxy<String> create(String name, View view) {
    return ProofListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  ProofListIndexProxy<String> createInGroup(String groupName, byte[] idInGroup, View view) {
    return ProofListIndexProxy.newInGroupUnsafe(groupName, idInGroup, view,
        StandardSerializers.string());
  }

  @Override
  StorageIndex createOfOtherType(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
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

  @Test
  void getProofSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, V1));
    });
  }

  @Test
  void getProofOfAbsenceEmptyList() {
    runTestWithView(database::createFork, (list) -> {
      assertThat(list, provesAbsence(0));
    });
  }

  @Test
  void getProofOfAbsenceSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesAbsence(1));
    });
  }

  @Test
  void getRangeProofOfAbsenceEmptyList() {
    runTestWithView(database::createFork, (list) -> {
      assertThat(list, provesAbsence(0, 1));
    });
  }

  @Test
  void getRangeProofOfAbsenceSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesAbsence(0, 2));
    });
  }

  @Test
  void getRangeProofSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, singletonList(V1)));
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 5, 7, 8, 9})
  void getProofMultipleItemList(int size) {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values.subList(0, size);

      list.addAll(values);

      for (int i = 0; i < values.size(); i++) {
        assertThat(list, provesThatContains(i, values.get(i)));
      }
    });
  }

  @Test
  void getRangeProofMultipleItemList_FullRange() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      assertThat(list, provesThatContains(0, values));
    });
  }

  @Test
  void getRangeProofMultipleItemList_1stHalf() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = 0;
      int to = values.size() / 2;
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  @Test
  void getRangeProofMultipleItemList_2ndHalf() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = values.size() / 2;
      int to = values.size();
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      Consumer<ProofListIndexProxy<String>> listTest) {
    runTestWithView(viewFactory, (ignoredView, list) -> listTest.accept(list));
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      BiConsumer<View, ProofListIndexProxy<String>> listTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        LIST_NAME,
        ProofListIndexProxy::newInstance,
        listTest
    );
  }
}
