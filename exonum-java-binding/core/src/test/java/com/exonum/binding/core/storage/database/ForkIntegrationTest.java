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

import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.indices.ListIndex;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import java.util.Iterator;

import org.junit.jupiter.api.Disabled;
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
          () -> assertThrows(IllegalStateException.class, fork::getViewNativeHandle),
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
      assertThrows(IllegalStateException.class, fork::getViewNativeHandle);
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
        () -> assertThrows(IllegalStateException.class, fork::getViewNativeHandle),
        () -> assertThrows(IllegalStateException.class, list1::size),
        () -> assertThrows(IllegalStateException.class, list2::size),
        () -> assertThrows(IllegalStateException.class, it::next)
    );
  }

  // TODO: Fix this test
  @Disabled
  @Test
  void rollbacksChangesMadeSinceLastCheckpoint() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);

      ListIndex<String> list = newList("list", fork);
      list.add("string1");

      fork.createCheckpoint();

      list.add("string2");
      assertEquals(2, list.size());

      fork.rollback();

      assertEquals(1, list.size());
      assertEquals("string1", list.get(0));
    }
  }

  // TODO: Fix this test
  @Disabled
  @Test
  void rollbackDoesNotAffectDatabase() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
         Cleaner cleaner = new Cleaner("cleaner")) {
      final String indexName = "list";

      Fork fork1 = db.createFork(cleaner);
      ListIndex<String> list1 = newList(indexName, fork1);
      list1.add("string1");
      db.merge(fork1);

      Fork fork2 = db.createFork(cleaner);
      ListIndex<String> list2 = newList(indexName, fork2);
      assertEquals(1, list2.size());
      assertEquals("string1", list2.get(0));
      list2.add("string2");
      list2.add("string3");
      assertEquals(3, list2.size());

      fork2.rollback();

      // Only changes from first fork persist in the database, because
      // second fork was rolled back.
      assertEquals(1, list2.size());
      assertEquals("string1", list2.get(0));
    }
  }

  // TODO: Fix this test
  @Disabled
  @Test
  void rollbacksAllChangesIfNoCheckpointWasCreated() throws Exception {
    try (TemporaryDb db = TemporaryDb.newInstance();
         Cleaner cleaner = new Cleaner("parent")) {
      Fork fork = db.createFork(cleaner);

      ListIndex<String> list = newList("list", fork);
      list.add("string1");

      fork.rollback();

      ListIndex<String> list2 = newList("list", fork);
      assertEquals(0, list2.size());
    }
  }

  private static ListIndex<String> newList(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }
}
