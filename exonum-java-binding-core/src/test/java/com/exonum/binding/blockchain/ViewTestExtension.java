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

package com.exonum.binding.blockchain;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * This extension allows to easily use Views in tests.
 *
 * After annotating the test class with <code>@ExtendWith(ViewTestExtension.class)</code> user is
 * able to write tests like this:
 *
 * <pre><code>
 * @BeforeEach
 * void setUp(MemoryDb db) {
 *   // Use db if needed
 * }
 *
 * @Test
 * void test(View view) {
 *   // Test logic
 * }
 * </code></pre>
 *
 * instead of:
 *
 * <pre><code>
 * MemoryDb db;
 *
 * @BeforeEach
 * void setUp() {
 *   db = MemoryDb.newInstance();
 *   // Use db if needed
 * }
 *
 * @Test
 * void test() {
 *   try (MemoryDb db = MemoryDb.newInstance();
 *        Cleaner cleaner = new Cleaner()) {
 *     View view = db.createSnapshot(cleaner);
 *     // Test logic
 *   }
 * }
 * </code></pre>
 */
public class ViewTestExtension implements ParameterResolver, BeforeEachCallback {

  private static final String KEY = "ResourceKey";

  static {
    LibraryLoader.load();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    MemoryDb db = MemoryDb.newInstance();
    Cleaner cleaner = new Cleaner();
    getStore(extensionContext).put(KEY, new CloseableDbAndCleaner(db, cleaner));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == MemoryDb.class
        || parameterContext.getParameter().getType() == View.class
        || parameterContext.getParameter().getType() == Snapshot.class
        || parameterContext.getParameter().getType() == Fork.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    CloseableDbAndCleaner resources =
        getStore(extensionContext).get(KEY, CloseableDbAndCleaner.class);
    MemoryDb memoryDb = resources.getMemoryDb();
    if (parameterContext.getParameter().getType() == MemoryDb.class) {
      return memoryDb;
    }
    else {
      Cleaner cleaner = resources.getCleaner();
      if (parameterContext.getParameter().getType() == Fork.class) {
        return memoryDb.createFork(cleaner);
      } else {
        return memoryDb.createSnapshot(cleaner);
      }
    }
  }

  private Store getStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  private static class CloseableDbAndCleaner implements CloseableResource {

    private final MemoryDb memoryDb;
    private final Cleaner cleaner;

    CloseableDbAndCleaner(MemoryDb memoryDb, Cleaner cleaner) {
      this.memoryDb = memoryDb;
      this.cleaner = cleaner;
    }

    MemoryDb getMemoryDb() {
      return memoryDb;
    }

    Cleaner getCleaner() {
      return cleaner;
    }

    @Override
    public void close() throws Throwable {
      cleaner.close();
    }

  }

}
