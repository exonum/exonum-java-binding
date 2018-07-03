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

package com.exonum.binding.storage.database;

import static com.exonum.binding.storage.indices.TestStorageItems.K2;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.MapIndexProxy;
import com.exonum.binding.storage.indices.TestStorageItems;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.exonum.binding.util.LibraryLoader;
import java.util.List;
import org.junit.Test;

public class MemoryDbIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void databaseMustClosePromptly() {
    MemoryDb database = MemoryDb.newInstance();
    database.close();  // No exceptions.
  }

  @Test
  public void getSnapshotShallCreateNonNullSnapshot() throws Exception {
    try (MemoryDb database = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot snapshot = database.createSnapshot(cleaner);
      assertNotNull(snapshot);
    }
  }

  @Test
  public void getForkShallCreateNonNullFork() throws Exception {
    try (MemoryDb database = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork fork = database.createFork(cleaner);
      assertNotNull(fork);
    }
  }

  @Test
  public void merge_singleList() throws Exception {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      String listName = "list";

      // Make changes to the list in the database.
      Fork fork = db.createFork(cleaner);
      ListIndex<String> list1 = newList(listName, fork);
      list1.add(V1);

      // Merge the patch.
      db.merge(fork);

      // Check the changes were successfully applied.
      Snapshot snapshot = db.createSnapshot(cleaner);
      ListIndex<String> list2 = newList(listName, snapshot);
      assertThat(list2.size(), equalTo(1L));
      assertThat(list2.get(0), equalTo(V1));
    }
  }

  @Test
  public void merge_twoIndices() throws Exception {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      String listName = "list";
      String mapName = "map";

      // Make changes to the indices in the database.
      Fork fork = db.createFork(cleaner);
      {
        ListIndex<String> list = newList(listName, fork);
        MapIndex<String, String> map = newMap(mapName, fork);
        list.add(V1);
        map.put(K2, V2);
      }

      // Merge the patch.
      db.merge(fork);

      // Check the changes were successfully applied.
      Snapshot snapshot = db.createSnapshot(cleaner);
      {
        ListIndex<String> list = newList(listName, snapshot);
        MapIndex<String, String> map = newMap(mapName, snapshot);
        assertThat(list.size(), equalTo(1L));
        assertThat(list.get(0), equalTo(V1));

        assertThat(map.get(K2), equalTo(V2));
      }
    }
  }

  @Test
  public void merge_multipleForks() throws Exception {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      String listName = "list";

      List<String> values = TestStorageItems.values.subList(0, 3);

      for (String v : values) {
        Fork fork = db.createFork(cleaner);

        ListIndex<String> list = newList(listName, fork);
        list.add(v);

        db.merge(fork);
      }

      // Check that all changes were successfully applied and the list in the database
      // contains all the added values.
      Snapshot snapshot = db.createSnapshot(cleaner);
      ListIndex<String> list = newList(listName, snapshot);
      assertThat(list.size(), equalTo((long) values.size()));
      for (int i = 0; i < values.size(); i++) {
        assertThat(values.get(i), equalTo(list.get(i)));
      }
    }
  }

  private static ListIndex<String> newList(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  private static MapIndex<String, String> newMap(String name, View view) {
    return MapIndexProxy.newInstance(name, view, StandardSerializers.string(),
        StandardSerializers.string());
  }
}
