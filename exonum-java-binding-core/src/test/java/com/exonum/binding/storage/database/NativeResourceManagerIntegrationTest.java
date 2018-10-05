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

package com.exonum.binding.storage.database;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.util.LibraryLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * A couple of tests that verify that using an invalid handle from Java does not crash the VM,
 * but results in a descriptive RuntimeException.
 */
public class NativeResourceManagerIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void nativeResourceManagerShallThrowIfUnknownHandle() {
    long unknownNativeHandle = 0x110B;

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Invalid handle value: '110B'");
    Views.nativeFree(unknownNativeHandle);
  }

  @Test
  public void nativeResourceManagerShallThrowIfHandleUsedWithOtherType() throws Exception {
    try (Database database = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork f = database.createFork(cleaner);
      long viewNativeHandle = f.getViewNativeHandle();

      // Try to use a handle to fork to access a memory db.
      MemoryDb db2 = new MemoryDb(viewNativeHandle);
      expectedException.expect(RuntimeException.class);
      expectedException.expectMessage("Wrong type id for");
      db2.close();
    }
  }

  @Test
  public void nativeResourceManagerShallThrowIfHandleUsedAfterFree() throws Exception {
    long snapshotNativeHandle = 0;
    try (Database database = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot s = database.createSnapshot(cleaner);
      // Preserve the handle to the snapshot.
      snapshotNativeHandle = s.getViewNativeHandle();
    }

    // The snapshot created inside try/catch is freed at this point, therefore,
    // the handle is no longer valid and the snapshot must not be accessible.
    Cleaner cleaner = new Cleaner();
    Snapshot s = Snapshot.newInstance(snapshotNativeHandle, cleaner);

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Invalid handle value: '"
        + handleToHex(snapshotNativeHandle));
    ListIndexProxy.newInstance("foo", s, StandardSerializers.string());
    // No cleaner#close on purpose.
  }

  private static String handleToHex(long snapshotNativeHandle) {
    return Long.toHexString(snapshotNativeHandle).toUpperCase();
  }
}
