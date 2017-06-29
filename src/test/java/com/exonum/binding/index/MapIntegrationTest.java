package com.exonum.binding.index;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;

import com.exonum.binding.storage.connector.Connect;
import com.exonum.binding.storage.db.Database;
import com.exonum.binding.storage.db.MemoryDb;
import org.junit.Test;

public class MapIntegrationTest {

  static {
    // To have library `libjava_bindings` available by name,
    // add a path to the folder containing it to java.library.path,
    // e.g.: java -Djava.library.path=rust/target/release …
    System.loadLibrary("java_bindings");
  }

  private static final byte[] mapPrefix = new byte[]{'p'};

  @Test
  public void getShouldReturnSuccessfullyPutValue() throws Exception {
    Database database = null;
    Connect view = null;
    try {
      database = new MemoryDb();
      view = database.lookupFork();

      Map map = new Map(view, mapPrefix);

      byte[] key = new byte[] {1};
      byte[] value = new byte[] {1, 2, 3, 4};
      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    } finally {
      if (view != null) {
        view.close();
      }
      if (database != null) {
        database.destroyNativeDb();
      }
    }
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInFork() throws Exception {
    Database database = null;
    Connect view = null;
    try {
      database = new MemoryDb();
      view = database.lookupFork();

      byte[] mapPrefix = new byte[] {'p'};
      Map map = new Map(view, mapPrefix);

      byte[] key = new byte[] {1};
      byte[] value = map.get(key);

      assertNull(value);
    } finally {
      if (view != null) {
        view.close();
      }
      if (database != null) {
        database.destroyNativeDb();
      }
    }
  }
}
