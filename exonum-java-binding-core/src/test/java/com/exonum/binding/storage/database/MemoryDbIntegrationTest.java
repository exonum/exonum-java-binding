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

  @Ignore
  @Test
  public void merge_singleList() throws Exception {
    try (MemoryDb db = new MemoryDb()) {
      String listName = "list";

      try (Fork fork = db.createFork();
           ListIndex list = new ListIndexProxy(listName, fork)) {
        list.add(V1);

        db.merge(fork);
      }

      try (Snapshot snapshot = db.createSnapshot();
           ListIndex list = new ListIndexProxy(listName, snapshot)) {
        assertThat(list.size(), equalTo(1L));
        assertThat(list.get(0), equalTo(V1));
      }
    }
  }

  @Ignore
  @Test
  public void merge_twoIndices() throws Exception {
    try (MemoryDb db = new MemoryDb()) {
      String listName = "list";
      String mapName = "map";

      try (Fork fork = db.createFork();
           ListIndex list = new ListIndexProxy(listName, fork);
           MapIndex map = new MapIndexProxy(mapName, fork)) {
        list.add(V1);
        map.put(K2, V2);

        db.merge(fork);
      }

      try (Snapshot snapshot = db.createSnapshot();
           ListIndex list = new ListIndexProxy(listName, snapshot);
           MapIndex map = new MapIndexProxy(mapName, snapshot)) {
        assertThat(list.size(), equalTo(1L));
        assertThat(list.get(0), equalTo(V1));

        assertThat(map.get(K2), equalTo(V2));
      }
    }
  }

  @Ignore
  @Test
  public void merge_multipleForks() throws Exception {
    try (MemoryDb db = new MemoryDb()) {
      String listName = "list";

      List<byte[]> values = TestStorageItems.values.subList(0, 3);

      for (byte[] v : values) {
        try (Fork fork = db.createFork();
             ListIndex list = new ListIndexProxy(listName, fork)) {
          list.add(v);
          db.merge(fork);
        }
      }

      try (Snapshot snapshot = db.createSnapshot();
           ListIndex list = new ListIndexProxy(listName, snapshot)) {
        assertThat(list.size(), equalTo(values.size()));
        for (int i = 0; i < values.size(); i++) {
          assertThat(values.get(i), equalTo(list.get(i)));
        }
      }
    }
  }
}
