package com.exonum.binding.storage.database;

import static org.junit.Assert.assertNotNull;

import com.exonum.binding.util.LibraryLoader;
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
}
