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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({
    Views.class,
})
class SnapshotTest {

  @Nested
  @Disabled
  // TODO Won't run with JUnit 5 till Powermock is updated [ECR-1614] (downgrade to 4?)
  class DestroysPeersIfNeeded {
    @BeforeEach
    void setUp() {
      mockStatic(Views.class);
    }

    @Test
    void destroy_NotOwning() throws Exception {
      try (Cleaner cleaner = new Cleaner()) {
        Snapshot.newInstance(0x0A, false, cleaner);
      }

      verifyStatic(Views.class, never());
      Views.nativeFree(anyLong());
    }

    @Test
    void destroy_Owning() throws Exception {
      int nativeHandle = 0x0A;

      try (Cleaner cleaner = new Cleaner()) {
        Snapshot.newInstance(nativeHandle, true, cleaner);
      }

      verifyStatic(Views.class);
      Views.nativeFree(nativeHandle);
    }
  }

  @Test
  void hasImmutableModificationCounter() throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      Snapshot s = Snapshot.newInstance(0x0A, false, cleaner);

      ModificationCounter c = s.getModificationCounter();
      assertThrows(IllegalStateException.class, c::notifyModified);
    }
  }
}
