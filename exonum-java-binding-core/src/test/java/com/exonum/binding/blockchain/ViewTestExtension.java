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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
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
 * void test(View view) {
 *   // Test logic
 * }
 * </code></pre>
 *
 * instead of:
 *
 * <pre><code>
 * void test() {
 *   try (MemoryDb db = MemoryDb.newInstance();
 *        Cleaner cleaner = new Cleaner()) {
 *     View view = db.createSnapshot(cleaner);
 *     // Test logic
 *   }
 * }
 * </code></pre>
 */
public class ViewTestExtension implements ParameterResolver, BeforeEachCallback,
    AfterEachCallback {

  private Cleaner cleaner;
  private Snapshot snapshot = null;
  private Fork fork = null;

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    MemoryDb db = MemoryDb.newInstance();
    cleaner = new Cleaner();
    snapshot = db.createSnapshot(cleaner);
    fork = db.createFork(cleaner);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    cleaner.close();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == View.class
        || parameterContext.getParameter().getType() == Snapshot.class
        || parameterContext.getParameter().getType() == Fork.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (parameterContext.getParameter().getType() == Fork.class) {
      return fork;
    } else {
      return snapshot;
    }
  }

}
