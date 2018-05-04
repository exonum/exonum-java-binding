package com.exonum.binding.storage.indices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// TODO: Move all tests applicable to any index in here when ECR-642 (JUnit 5) is resolved.
// Currently it's not possible due to JUnit 4 limitations (e.g., @Rules do not work).
abstract class BaseIndexProxyTestable<IndexT extends StorageIndex> {

  static {
    LibraryLoader.load();
  }

  MemoryDb database;

  @Before
  public void setUp() throws Exception {
    database = MemoryDb.newInstance();
  }

  @After
  public void tearDown() throws Exception {
    if (database != null) {
      database.close();
    }
  }

  abstract IndexT create(String name, View view);

  @Test
  public void getName() throws CloseFailuresException {
    String name = "test_index";
    try (Cleaner cleaner = new Cleaner()) {
      View view = database.createSnapshot(cleaner);
      IndexT index = create(name, view);

      assertThat(index.getName(), equalTo(name));
    }
  }

  @Test
  public void toStringIncludesNameAndType() throws CloseFailuresException {
    String name = "test_index";
    try (Cleaner cleaner = new Cleaner()) {
      View view = database.createSnapshot(cleaner);
      IndexT index = create(name, view);

      String indexInfo = index.toString();
      assertThat(indexInfo, containsString(name));
      String className = index.getClass().getSimpleName();
      assertThat(indexInfo, containsString(className));
    }
  }
}
