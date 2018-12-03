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

  @Test
  void getIfValid() {
    long handle = 0x11L;
    nativeHandle = new NativeHandle(handle);

    assertTrue(nativeHandle.isValid());
    assertThat(nativeHandle.get()).isEqualTo(handle);
  }

  @Test
  void getInvalid() {
    long handle = NativeHandle.INVALID_NATIVE_HANDLE;
    nativeHandle = new NativeHandle(handle);

    assertFalse(nativeHandle.isValid());
    assertThrows(IllegalStateException.class, () -> nativeHandle.get());
  }

  @Test
  void close() {
    long handle = 0x11L;
    nativeHandle = new NativeHandle(handle);

    nativeHandle.close();
    assertFalse(nativeHandle.isValid());
  }

  @Test
  void closeMultipleTimes() {
    long handle = 0x11L;
    nativeHandle = new NativeHandle(handle);

    nativeHandle.close();
    nativeHandle.close();
    assertFalse(nativeHandle.isValid());
  }

  @Test
  public void toStringHexRepresentation() {
    long handle = 0x11L;
    nativeHandle = new NativeHandle(handle);

    assertThat(nativeHandle.toString()).contains(Long.toHexString(handle).toUpperCase());
  }
}
