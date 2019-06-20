/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.storage.database;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import org.junit.jupiter.api.Test;

/**
 * A couple of tests that verify that using an invalid handle from Java does not crash the VM,
 * but results in a descriptive RuntimeException.
 */
@RequiresNativeLibrary
class NativeResourceManagerIntegrationTest {

  @Test
  void nativeResourceManagerShallThrowIfUnknownHandle() {
    long unknownNativeHandle = 0x110B;


    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> Views.nativeFree(unknownNativeHandle));
    assertThat(thrown).hasMessage("Invalid handle value: '110B'");
  }

  @Test
  void nativeResourceManagerShallThrowIfHandleUsedWithOtherType() throws Exception {
    try (Database database = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork f = database.createFork(cleaner);
      long viewNativeHandle = f.getViewNativeHandle();

      // Try to use a handle to fork to access a memory db.
      MemoryDb db2 = new MemoryDb(viewNativeHandle);

      RuntimeException thrown = assertThrows(RuntimeException.class, db2::close);
      assertThat(thrown).hasMessageContaining("Wrong type id for");
    }
  }

  @Test
  void nativeResourceManagerShallThrowIfHandleUsedAfterFree() throws Exception {
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

    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> ListIndexProxy.newInstance("foo", s, StandardSerializers.string()));
    assertThat(thrown).hasMessageContaining("Invalid handle value: '"
        + handleToHex(snapshotNativeHandle));
    // No cleaner#close on purpose.
  }

  private static String handleToHex(long snapshotNativeHandle) {
    return Long.toHexString(snapshotNativeHandle).toUpperCase();
  }
}
