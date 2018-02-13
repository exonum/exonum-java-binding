package com.exonum.binding.storage.database;

import static com.exonum.binding.storage.indices.TestStorageItems.K2;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.MapIndexProxy;
import com.exonum.binding.storage.indices.TestStorageItems;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.exonum.binding.util.LibraryLoader;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;

public class MemoryDbIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void databaseMustClosePromptly() throws Exception {
    MemoryDb database = new MemoryDb();
    database.close();  // No exceptions.
  }

  @Test
  public void getSnapshotShallCreateNonNullSnapshot() throws Exception {
    try (MemoryDb database = new MemoryDb();
         Snapshot snapshot = database.createSnapshot()) {
      assertNotNull(snapshot);
    }
  }

  @Test
  public void getForkShallCreateNonNullFork() throws Exception {
    try (MemoryDb database = new MemoryDb();
         Fork fork = database.createFork()) {
      assertNotNull(fork);
    }
  }

  @Test
  public void merge_singleList() throws Exception {
    try (MemoryDb db = new MemoryDb()) {
      String listName = "list";

      try (Fork fork = db.createFork();
           ListIndex<String> list = newList(listName, fork)) {
        list.add(V1);

        db.merge(fork);
      }

      try (Snapshot snapshot = db.createSnapshot();
           ListIndex<String> list = newList(listName, snapshot)) {
        assertThat(list.size(), equalTo(1L));
        assertThat(list.get(0), equalTo(V1));
      }
    }
  }

  @Test
  public void merge_twoIndices() throws Exception {
    try (MemoryDb db = new MemoryDb()) {
      String listName = "list";
      String mapName = "map";

      try (Fork fork = db.createFork();
           ListIndex<String> list = newList(listName, fork);
           MapIndex<String, String> map = newMap(mapName, fork)) {
        list.add(V1);
        map.put(K2, V2);

        db.merge(fork);
      }

      try (Snapshot snapshot = db.createSnapshot();
           ListIndex<String> list = newList(listName, snapshot);
           MapIndex<String, String> map = newMap(mapName, snapshot)) {
        assertThat(list.size(), equalTo(1L));
        assertThat(list.get(0), equalTo(V1));

        assertThat(map.get(K2), equalTo(V2));
      }
    }
  }

  @Test
  public void merge_multipleForks() throws Exception {
    try (MemoryDb db = new MemoryDb()) {
      String listName = "list";

      List<String> values = TestStorageItems.values.subList(0, 3);

      for (String v : values) {
        try (Fork fork = db.createFork();
             ListIndex<String> list = newList(listName, fork)) {
          list.add(v);
          db.merge(fork);
        }
      }

      try (Snapshot snapshot = db.createSnapshot();
           ListIndex<String> list = newList(listName, snapshot)) {
        assertThat(list.size(), equalTo((long) values.size()));
        for (int i = 0; i < values.size(); i++) {
          assertThat(values.get(i), equalTo(list.get(i)));
        }
      }
    }
  }

  private static ListIndex<String> newList(String name, View view) {
    return new ListIndexProxy<>(name, view, StandardSerializers.string());
  }

  private static MapIndex<String, String> newMap(String name, View view) {
    return new MapIndexProxy<>(name, view, StandardSerializers.string(),
        StandardSerializers.string());
  }
}
