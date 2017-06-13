package com.exonum.binding.index;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;

import com.exonum.binding.storage.connector.Connect;
import com.exonum.binding.storage.db.Database;
import com.exonum.binding.storage.db.MemoryDb;
import org.junit.Test;

public class IndexMapIntegrationTest {

  static {
    // To have library `libjava_bindings` available by name,
    // add a path to the folder containing it to java.library.path,
    // e.g.: java -Djava.library.path=rust/target/release …
    System.loadLibrary("java_bindings");
  }

  // todo: remove expected exception when native code is fixed.
  @Test(expected = AssertionError.class)
  public void getShouldReturnSuccessfullyPutValue() throws Exception {
    fail();
    TestStorageKey key = new TestStorageKey();
    TestStorageValue value = new TestStorageValue();
    byte[] mapPrefix = new byte[] {'p'};
    Database database = null;
    Connect view = null;
    try {
      database = new MemoryDb();
      view = database.lookupFork();

      IndexMap<TestStorageKey, TestStorageValue> map =
              new IndexMap<>(TestStorageValue.class, view, mapPrefix);
      map.put(key, value);

      TestStorageValue storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    } finally {
      if (view != null) {
        view.destroyNativeConnect();
      }
      if (database != null) {
        database.destroyNativeDb();
      }
    }
  }
}
