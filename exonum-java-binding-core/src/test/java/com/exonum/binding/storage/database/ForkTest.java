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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.exonum.binding.proxy.Cleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({
    Views.class,
})
@Disabled
// TODO Won't run on Junit 5 till Powermock is updated [ECR-1614].
class ForkTest {

  private Fork fork;

  @BeforeEach
  void setUp() {
    mockStatic(Views.class);
  }

  @Test
  void disposeInternal_OwningProxy() throws Exception {
    int nativeHandle = 0x0A;
    try (Cleaner cleaner = new Cleaner()) {
      fork = Fork.newInstance(nativeHandle, true, cleaner);
    }

    verifyStatic(Views.class);
    Views.nativeFree(nativeHandle);
  }

  @Test
  void disposeInternal_NotOwningProxy() throws Exception {
    int nativeHandle = 0x0A;

    try (Cleaner cleaner = new Cleaner()) {
      fork = Fork.newInstance(nativeHandle, false, cleaner);
    }

    verifyStatic(Views.class, never());
    Views.nativeFree(nativeHandle);
  }


  @Test
  void canModify() {
    Fork fork = Fork.newInstance(0x0A, false, new Cleaner());
    assertFalse(fork.canModify());
  }
}
