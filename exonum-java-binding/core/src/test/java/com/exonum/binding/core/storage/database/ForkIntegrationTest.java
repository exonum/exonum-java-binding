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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.indices.ListIndex;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class ForkIntegrationTest {

  @Test
  void mergeInvalidatesForkAndDependencies() throws Exception {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner("parent")) {
      // Create some indexes and an iterator using a fork.
      // Each of them shall be registered with the internal Fork cleaner,
      // ensuring destruction on intoPatch (MemoryDb#merge)
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
  void forkRegisteredProxiesAreInvalidatedWhenParentCleanerClosed() throws Exception {
    Fork fork;
    ListIndex<String> list1;
    ListIndex<String> list2;
    Iterator<String> it;

    try (MemoryDb db = MemoryDb.newInstance();
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

  private static ListIndex<String> newList(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }
}
