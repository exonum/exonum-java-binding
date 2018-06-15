/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
