package com.exonum.binding.proxy;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class MemoryDbIntegrationTest {

  static {
    // To have library `libjava_bindings` available by name,
    // add a path to the folder containing it to java.library.path,
    // e.g.: java -Djava.library.path=rust/target/release …
    System.loadLibrary("java_bindings");
    // TODO(dt): Replace with a library loader.
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
