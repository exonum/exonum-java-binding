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
import static com.exonum.binding.core.storage.indices.IndexAddress.valueOf;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.database.Snapshot;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Base class for common {@link EntryIndex} operations.
 */
abstract class BaseEntryIndexProxyIntegrationTest<IndexT extends EntryIndex<String>>
    extends BaseIndexProxyTestable<IndexT> {

  private static final String ENTRY_NAME = "test_entry";
  public static final Serializer<String> SERIALIZER = StandardSerializers.string();

  @Test
  void entryGroupsAreNotSupported() throws CloseFailuresException {
    try (Cleaner c = new Cleaner()) {
      Snapshot snapshot = database.createSnapshot(c);
      IndexAddress addressInGroup = IndexAddress.valueOf("test", bytes("id"));
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> snapshot.getProofEntry(addressInGroup, SERIALIZER));
      assertThat(e.getMessage(), containsString("Groups of Entries are not supported"));
    }
  }

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

  void runTestWithView(Function<Cleaner, Access> viewFactory,
      Consumer<IndexT> entryTest) {
    runTestWithView(viewFactory, (ignoredView, entry) -> entryTest.accept(entry));
  }

  void runTestWithView(Function<Cleaner, Access> viewFactory,
      BiConsumer<Access, IndexT> entryTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        ENTRY_NAME,
        (address, access, serializer) -> create(address.getName(), access),
        entryTest
    );
  }

  @Override
  StorageIndex createOfOtherType(String name, Access access) {
    return access.getList(valueOf(name), string());
  }

  @Override
  Object getAnyElement(IndexT index) {
    return index.get();
  }

  @Override
  void update(IndexT index) {
    index.set(V1);
  }
}
