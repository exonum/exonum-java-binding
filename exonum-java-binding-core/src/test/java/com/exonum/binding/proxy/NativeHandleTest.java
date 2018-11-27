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

package com.exonum.binding.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NativeHandleTest {

  private NativeHandle nativeHandle;

  private static long HANDLE = 0x11L;

  private static final String NATIVE_HANDLE = "11";

  @Test
  void getIfValid() {
    nativeHandle = new NativeHandle(HANDLE);

    assertTrue(nativeHandle.isValid());
    assertThat(nativeHandle.get()).isEqualTo(HANDLE);
  }

  @Test
  void getInvalid() {
    long handle = NativeHandle.INVALID_NATIVE_HANDLE;
    nativeHandle = new NativeHandle(handle);

    assertThrows(IllegalStateException.class, () -> nativeHandle.get());
  }

  @Test
  void close() {
    nativeHandle = new NativeHandle(HANDLE);

    nativeHandle.close();
    assertFalse(nativeHandle.isValid());
    assertThat(nativeHandle.toString()).contains(NATIVE_HANDLE);
  }

  @Test
  void closeMultipleTimes() {
    nativeHandle = new NativeHandle(HANDLE);

    nativeHandle.close();
    nativeHandle.close();
    assertFalse(nativeHandle.isValid());
    assertThat(nativeHandle.toString()).contains(NATIVE_HANDLE);
  }

  @Test
  void toStringHexRepresentation() {
    nativeHandle = new NativeHandle(HANDLE);

    assertThat(nativeHandle.toString()).contains(NATIVE_HANDLE);
  }
}
