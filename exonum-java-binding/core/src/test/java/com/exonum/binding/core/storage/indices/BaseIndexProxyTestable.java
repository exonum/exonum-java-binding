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

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Database;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.MemoryDb;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.test.RequiresNativeLibrary;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// TODO: Move all tests applicable to any index in here when ECR-642 (JUnit 5) is resolved.
// Currently it's not possible due to JUnit 4 limitations (e.g., @Rules do not work).
@RequiresNativeLibrary
abstract class BaseIndexProxyTestable<IndexT extends StorageIndex> {

  MemoryDb database;

  @BeforeEach
  void setUp() {
    database = MemoryDb.newInstance();
  }

  @AfterEach
  void tearDown() {
    if (database != null) {
      database.close();
    }
  }

  abstract IndexT create(String name, View view);

  abstract @Nullable IndexT createInGroup(String groupName, byte[] idInGroup, View view);

  abstract StorageIndex createOfOtherType(String name, View view);

  /**
   * Get any element from this index.
   */
  abstract Object getAnyElement(IndexT index);

  /**
   * Performs a modifying operation on the index.
   */
  abstract void update(IndexT index);

  /**
   * A test verifying that an index constructor adds its destructor to the cleaner.
   * First it checks the number of actions registered before and after the constructor is executed,
   * and then that the index becomes inaccessible after the cleaner is closed.
   */
  @Test
  void indexConstructorRegistersItsDestructor() throws CloseFailuresException {
    String name = "test_index";

    try (Cleaner cleaner = new Cleaner()) {
      View view = database.createSnapshot(cleaner);

      int numAddedActions = cleaner.getNumRegisteredActions();
      IndexT index = create(name, view);

      // Check that the index constructor registered a single clean action.
      int numActionsExpected = numAddedActions + 1;
      assertThat(cleaner.getNumRegisteredActions(), equalTo(numActionsExpected));

      // Close the cleaner (it’s OK to do that inside try-with-resources
      // since this method is idempotent).
      cleaner.close();

      // Try to access the index
      assertThrows(IllegalStateException.class, () -> getAnyElement(index));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      //      " name", // FIXME: commented out until ECR-3345
      //      "name ",
      //      "name 1",
      //      " name ",
      //      "?name",
      //      "name?",
      //      "na?me",
      //      "name#1",
      //      "name-1",
  })
  void indexConstructorThrowsIfInvalidName(String name) throws Exception {
    try (Cleaner cleaner = new Cleaner();
        Database database = MemoryDb.newInstance()) {
      Snapshot view = database.createSnapshot(cleaner);

      assertThrows(Exception.class, () -> create(name, view));
    }
  }

  @Test
  void indexConstructorAllowsMultipleInstancesFromFork() throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      String name = "test_index";
      Fork view = database.createFork(cleaner);
      // Create two indices with the same name from the same Fork. It is disallowed currently
      // in Rust, but must work in Java with indices performing instance de-duplication.
      IndexT i1 = create(name, view);
      IndexT i2 = create(name, view);

      assertNotNull(i1);
      assertThat(i2, sameInstance(i1));
    }
  }

  @Test
  void indexConstructorAllowsMultipleInstancesFromForkInGroup() throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      String name = "test_index";
      byte[] idInGroup = bytes("index id in the group");
      Fork view = database.createFork(cleaner);
      // Create two indices with the same name from the same Fork. It is disallowed currently
      // in Rust, but must work in Java with indices performing instance de-duplication.
      IndexT i1 = createInGroup(name, idInGroup, view);
      IndexT i2 = createInGroup(name, idInGroup, view);

      assumeFalse(i1 == null, "Groups are not supported by EntryIndex");
      assertThat(i2, sameInstance(i1));
    }
  }

  @Test
  void indexConstructorThrowsIfIndexWithSameNameButOtherTypeIsOpened()
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      String name = "test_index";
      Fork view = database.createFork(cleaner);

      // Try to create two indices of different types with the same name
      StorageIndex other = createOfOtherType(name, view);
      Exception e = assertThrows(IllegalArgumentException.class, () -> create(name, view));

      String message = e.getMessage();
      assertThat(message, containsString("Cannot create index"));
      assertThat(message, containsString(name));
      assertThat(message, containsString(String.valueOf(other)));
    }
  }

  /**
   * An integration test that ensures that:
   * - Constructor of this type preserves the index type information and
   * - Constructor of the other type checks it, preventing illegal access to the internals.
   */
  @Test
  @Disabled("Needs working Database#merge from ECR-3330")
  void indexConstructorPersistsIndexTypeInfo() throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      String name = "test_index";
      Fork view = database.createFork(cleaner);
      // Create an index with the given name
      IndexT index = create(name, view);
      update(index);

      // Merge the changes into the database
      database.merge(view);

      // Create a new Snapshot to be able to create another index with the same address
      Snapshot snapshot = database.createSnapshot(cleaner);
      // Try to create an index of other type with the same name as the index above
      Exception e = assertThrows(RuntimeException.class, () -> createOfOtherType(name, snapshot));

      // TODO: Change message after https://jira.bf.local/browse/ECR-3354
      assertThat(e, hasMessage(containsString("Index type doesn't match specified")));
    }
  }

  @Test
  void getName() throws CloseFailuresException {
    String name = "test_index";
    try (Cleaner cleaner = new Cleaner()) {
      View view = database.createSnapshot(cleaner);
      IndexT index = create(name, view);

      assertThat(index.getName(), equalTo(name));
    }
  }

  @Test
  void getAddress() throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      String name = "test_index";
      View view = database.createSnapshot(cleaner);
      IndexT index = create(name, view);

      IndexAddress expected = IndexAddress.valueOf(name);
      assertThat(index.getAddress(), equalTo(expected));
    }
  }

  @Test
  void getAddressInGroup() throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      String groupName = "test_index";
      byte[] idInGroup = bytes("prefix");
      View view = database.createSnapshot(cleaner);
      IndexT index = createInGroup(groupName, idInGroup, view);

      assumeFalse(index == null, "Groups are not supported by EntryIndex");

      IndexAddress address = IndexAddress.valueOf(groupName, idInGroup);
      assertThat(index.getAddress(), equalTo(address));
    }
  }

  @Test
  void toStringIncludesNameAndType() throws CloseFailuresException {
    String name = "test_index";
    try (Cleaner cleaner = new Cleaner()) {
      View view = database.createSnapshot(cleaner);
      IndexT index = create(name, view);

      String indexInfo = index.toString();
      assertThat(indexInfo, containsString(name));
      String className = index.getClass().getSimpleName();
      assertThat(indexInfo, containsString(className));
    }
  }
}
