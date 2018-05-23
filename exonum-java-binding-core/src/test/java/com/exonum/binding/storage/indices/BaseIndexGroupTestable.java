package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.util.LibraryLoader;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;

abstract class BaseIndexGroupTestable {

  static {
    LibraryLoader.load();
  }

  /** A default cleaner for a test case. */
  Cleaner cleaner;

  MemoryDb db;

  @Before
  public void setUp() {
    db = MemoryDb.newInstance();
    cleaner = new Cleaner();
  }

  @After
  public void tearDown() {
    Stream.of(cleaner, db)
        .filter(Objects::nonNull)
        .forEach(o -> close(o));
  }

  private static void close(AutoCloseable o) {
    try {
      o.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
