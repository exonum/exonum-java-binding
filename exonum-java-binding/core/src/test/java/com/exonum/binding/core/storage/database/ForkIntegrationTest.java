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

package com.exonum.binding.core.storage.database;

import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ListIndex;
import com.exonum.binding.test.RequiresNativeLibrary;
import java.util.Iterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class ForkIntegrationTest {

  @Test
  void mergeInvalidatesForkAndDependencies() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      // Create some indexes and an iterator using a fork.
      // Each of them shall be registered with the internal Fork cleaner,
      // ensuring destruction on intoPatch (TemporaryDb#merge)
      Fork fork = db.createFork(cleaner);
      ListIndex<String> list1 = newList("list_1", fork);
      list1.add(V1);
      ListIndex<String> list2 = newList("list_2", fork);
      list2.add(V2);
      Iterator<String> it = list1.iterator();

      // Merge the fork (involves converting into patch)
      db.merge(fork);

      // Check that all created native proxies are no longer accessible
      assertAll(
          () -> assertThrows(IllegalStateException.class, fork::getAccessNativeHandle),
          () -> assertThrows(IllegalStateException.class, list1::size),
          () -> assertThrows(IllegalStateException.class, list2::size),
          () -> assertThrows(IllegalStateException.class, it::next)
      );
    }
  }

  @Test
  void mergeAbortedIfCollectionsFailedToClose() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);
      // Create a 'normal' collection
      ListIndex<String> list1 = newList("list_1", fork);
      // Register a clean action with the *fork* that completes exceptionally
      Cleaner forkCleaner = fork.getCleaner();
      forkCleaner.add(() -> {
        throw new IllegalStateException("Evil clean action");
      });

      // Attempt to merge the fork (involves converting into patch, which must be aborted)
      Exception e = assertThrows(IllegalStateException.class, () -> db.merge(fork));

      assertThat(e)
          .hasMessageContaining("intoPatch aborted")
          .hasCauseInstanceOf(CloseFailuresException.class);

      // Check that the 'normal' collection and the fork are no longer accessible
      assertThrows(IllegalStateException.class, list1::size);
      assertThrows(IllegalStateException.class, fork::getAccessNativeHandle);
    }
  }

  @Test
  void forkRegisteredProxiesAreInvalidatedWhenParentCleanerClosed() throws Exception {
    Fork fork;
    ListIndex<String> list1;
    ListIndex<String> list2;
    Iterator<String> it;

    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      // Create some indexes and an iterator using a fork.
      // Each of them shall be registered with the Fork cleaner, which, in turn,
      // must be registered with the parent cleaner.
      fork = db.createFork(cleaner);
      list1 = newList("list_1", fork);
      list1.add(V1);
      list2 = newList("list_2", fork);
      list2.add(V2);
      it = list1.iterator();
    }
    // The parent Cleaner has been closed, and the fork must release all the dependencies.

    // Check that all created native proxies are no longer accessible, i.e.,
    // the internal Fork cleaner is properly registered with the parent cleaner.
    assertAll(
        () -> assertThrows(IllegalStateException.class, fork::getAccessNativeHandle),
        () -> assertThrows(IllegalStateException.class, list1::size),
        () -> assertThrows(IllegalStateException.class, list2::size),
        () -> assertThrows(IllegalStateException.class, it::next)
    );
  }

  @Test
  void rollbacksChangesMadeSinceLastCheckpoint() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);

      // Create a list with a single element
      String listName = "list";
      ListIndex<String> l1 = newList(listName, fork);
      l1.add("s1");

      // Create a checkpoint
      fork.createCheckpoint();

      ListIndex<String> l2 = newList(listName, fork);
      // Modify the list
      l2.add("s2");
      assertThat(l2).containsExactly("s1", "s2");

      // Rollback the changes
      fork.rollback();

      // Verify the state of the list is equal to the one just before
      // the checkpoint was created
      ListIndex<String> l3 = newList(listName, fork);
      assertThat(l3).containsExactly("s1");
    }
  }

  @Test
  @DisplayName("rollback shall work when multiple checkpoints are created, reverting to the"
      + "state as of the last checkpoint")
  void rollbacksToTheLastCheckpointWhenMultipleAreCreated() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);

      // Create a list with a single element
      String listName = "list";

      // Modify and create the first checkpoint
      {
        ListIndex<String> list = newList(listName, fork);
        list.add("s1");
        fork.createCheckpoint();
      }

      // Modify and create the second checkpoint
      {
        ListIndex<String> list = newList(listName, fork);
        list.add("s2");
        fork.createCheckpoint();
      }

      // Modify the list
      {
        ListIndex<String> list = newList(listName, fork);
        list.add("s3");
        assertThat(list).containsExactly("s1", "s2", "s3");
      }

      // Rollback the changes: must restore the state as of the second checkpoint
      fork.rollback();
      {
        ListIndex<String> list = newList(listName, fork);
        assertThat(list).containsExactly("s1", "s2");
      }

      // Rollback again: as no nested (stacked) checkpoints are supported,
      // the first checkpoint is no longer available and any rollback will revert
      // the state to the last (second) checkpoint
      fork.rollback();
      {
        ListIndex<String> list = newList(listName, fork);
        assertThat(list).containsExactly("s1", "s2");
      }
    }
  }

  @Test
  void rollbackDoesNotAffectDatabase() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
         Cleaner cleaner = new Cleaner("cleaner")) {
      String indexName = "list";

      Fork fork1 = db.createFork(cleaner);
      ListIndex<String> list1 = newList(indexName, fork1);
      list1.add("s1");
      // Merge the fork, so that the database has a list ["s1"]
      db.merge(fork1);

      Fork fork2 = db.createFork(cleaner);
      ListIndex<String> list2 = newList(indexName, fork2);
      list2.add("s2");
      list2.add("s3");
      assertThat(list2).containsExactly("s1", "s2", "s3");
      // Rollback the 2nd fork
      fork2.rollback();
      // and merge it (a fork with no changes)
      db.merge(fork2);

      // Only changes from the first fork persist in the database, because
      // second fork was rolled back.
      Snapshot s = db.createSnapshot(cleaner);
      ListIndex<String> list3 = newList(indexName, s);
      assertThat(list3).containsExactly("s1");
    }
  }

  @Test
  void rollbacksAllChangesIfNoCheckpointWasCreated() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
         Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);

      ListIndex<String> list = newList("list", fork);
      list.add("s1");

      fork.rollback();

      ListIndex<String> list2 = newList("list", fork);
      assertThat(list2).isEmpty();
    }
  }

  @Test
  void createCheckpointInvalidatesDependentObjects() throws CloseFailuresException {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);

      ListIndex<String> l1 = newList("test_list", fork);

      // Create a checkpoint
      fork.createCheckpoint();

      // Check the collections created before checkpoint are inaccessible
      assertThrows(IllegalStateException.class, l1::size);
    }
  }

  @Test
  void rollbackInvalidatesDependentObjects() throws CloseFailuresException {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);

      ListIndex<String> l1 = newList("test_list", fork);

      // Rollback
      fork.rollback();

      // Check the collections created before rollback are inaccessible
      assertThrows(IllegalStateException.class, l1::size);
    }
  }

  private static ListIndex<String> newList(String name, Access access) {
    return access.getList(IndexAddress.valueOf(name), string());
  }
}
