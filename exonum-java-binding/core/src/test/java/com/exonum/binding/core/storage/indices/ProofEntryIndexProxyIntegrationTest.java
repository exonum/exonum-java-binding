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

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.database.View;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ProofEntryIndexProxyIntegrationTest
    extends BaseIndexProxyTestable<ProofEntryIndexProxy<String>> {

  private static final String ENTRY_NAME = "test_entry";
  public static final Serializer<String> SERIALIZER = StandardSerializers.string();

  @Test
  void setValue() {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);

      assertTrue(e.isPresent());
      assertThat(e.get(), equalTo(V1));
    });
  }

  @Test
  void setOverwritesPreviousValue() {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);
      e.set(V2);

      assertTrue(e.isPresent());
      assertThat(e.get(), equalTo(V2));
    });
  }

  @Test
  void setFailsWithSnapshot() {
    runTestWithView(database::createSnapshot, (e) -> {
      assertThrows(UnsupportedOperationException.class, () -> e.set(V1));
    });
  }

  @Test
  void isNotInitiallyPresent() {
    runTestWithView(database::createSnapshot, (e) -> assertFalse(e.isPresent()));
  }

  @Test
  void getFailsIfNotPresent() {
    runTestWithView(database::createSnapshot,
        (e) -> assertThrows(NoSuchElementException.class, e::get));
  }

  @Test
  void getIndexHashEmptyEntry() {
    runTestWithView(database::createSnapshot, e -> {
      HashCode indexHash = e.getIndexHash();
      // Expected hash of an empty Entry: all zeroes
      HashCode expectedHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      assertThat(indexHash, equalTo(expectedHash));
    });
  }

  @Test
  void getIndexHashNonEmptyEntry() {
    runTestWithView(database::createFork, e -> {
      String value = V1;
      e.set(value);

      HashCode indexHash = e.getIndexHash();
      // Expected hash of a set Entry: SHA-256(value)
      byte[] valueAsBytes = SERIALIZER.toBytes(value);
      HashCode expectedHash = Hashing.sha256()
          .hashBytes(valueAsBytes);
      assertThat(indexHash, equalTo(expectedHash));
    });
  }

  @Test
  void removeIfNoValue() {
    runTestWithView(database::createFork, (e) -> {
      assertFalse(e.isPresent());
      e.remove();
      assertFalse(e.isPresent());
    });
  }

  @Test
  void removeValue() {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);
      e.remove();
      assertFalse(e.isPresent());
    });
  }

  @Test
  void removeFailsWithSnapshot() {
    runTestWithView(database::createSnapshot,
        (e) -> assertThrows(UnsupportedOperationException.class, e::remove));
  }

  @Test
  void toOptional() {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);
      Optional<String> optional = e.toOptional();
      assertTrue(optional.isPresent());
      assertThat(optional.get(), is(V1));
    });
  }

  @Test
  void optionalEmptyIfNoValue() {
    runTestWithView(database::createFork, (e) -> {
      assertFalse(e.isPresent());
      Optional<String> optional = e.toOptional();
      assertFalse(optional.isPresent());
    });
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
      Consumer<ProofEntryIndexProxy<String>> entryTest) {
    runTestWithView(viewFactory, (ignoredView, entry) -> entryTest.accept(entry));
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
      BiConsumer<View, ProofEntryIndexProxy<String>> entryTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        ENTRY_NAME,
        ProofEntryIndexProxy::newInstance,
        entryTest
    );
  }

  @Override
  ProofEntryIndexProxy<String> create(String name, View view) {
    return ProofEntryIndexProxy.newInstance(name, view, SERIALIZER);
  }

  @Override
  ProofEntryIndexProxy<String> createInGroup(String groupName, byte[] idInGroup, View view) {
    return null; // Entry index does not support groups
  }

  @Override
  StorageIndex createOfOtherType(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  Object getAnyElement(ProofEntryIndexProxy<String> index) {
    return index.get();
  }

  @Override
  void update(ProofEntryIndexProxy<String> index) {
    index.set(V1);
  }
}
