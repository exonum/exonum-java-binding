package com.exonum.binding.proxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MapIndexProxyIntegrationTest {

  static {
    // To have library `libjava_bindings` available by name,
    // add a path to the folder containing it to java.library.path,
    // e.g.: java -Djava.library.path=rust/target/release …
    System.loadLibrary("java_bindings");
  }

  private static final byte[] mapPrefix = new byte[]{'p'};

  @Test
  public void getShouldReturnSuccessfullyPutValue() throws Exception {
    try (Database database = new MemoryDb();
         Connect view = database.lookupFork();
         MapIndexProxy map = new MapIndexProxy(view, mapPrefix)) {
      byte[] key = new byte[]{1};
      byte[] value = new byte[]{1, 2, 3, 4};

      map.put(key, value);

      byte[] storedValue = map.get(key);

      assertThat(storedValue, equalTo(value));
    }
  }

  @Test
  public void getShouldReturnNullIfNoSuchValueInFork() throws Exception {
    byte[] mapPrefix = new byte[] {'p'};
    try (Database database = new MemoryDb();
         Connect view = database.lookupFork();
         MapIndexProxy map = new MapIndexProxy(view, mapPrefix)) {
      byte[] key = new byte[]{1};
      byte[] value = map.get(key);

      assertNull(value);
    }
  }
}
