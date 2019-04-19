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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// TODO: Move all tests applicable to any index in here when ECR-642 (JUnit 5) is resolved.
// Currently it's not possible due to JUnit 4 limitations (e.g., @Rules do not work).
@RequiresNativeLibrary
abstract class BaseIndexProxyTestable<IndexT extends StorageIndex> {

  static {
    LibraryLoader.load();
  }

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

  /**
   * Get any element from this index.
   */
  abstract Object getAnyElement(IndexT index);

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
      " name",
      "name ",
      "name 1",
      " name ",
      "?name",
      "name?",
      "na?me",
      "name#1",
      "name-1",
  })
  void indexConstructorThrowsIfInvalidName(String name) throws Exception {
    try (Cleaner cleaner = new Cleaner();
        Database database = MemoryDb.newInstance()) {
      Snapshot view = database.createSnapshot(cleaner);

      assertThrows(Exception.class, () -> create(name, view));
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
