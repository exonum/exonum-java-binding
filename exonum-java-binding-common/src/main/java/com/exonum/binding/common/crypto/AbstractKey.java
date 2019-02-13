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

package com.exonum.binding.common.crypto;

import static com.exonum.binding.common.crypto.CryptoUtils.byteArrayToHex;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;

/**
 * Represent either a private or public key in a digital signature system.
 */
public abstract class AbstractKey {

  private final byte[] rawKey;

  AbstractKey(byte[] rawKey) {
    checkArgument(rawKey.length > 0, "Key must not be empty");
    this.rawKey = rawKey;
  }

  /**
   * Returns the value of this key as a byte array.
   */
  public byte[] toBytes() {
    return rawKey.clone();
  }

  /**
   * Returns a mutable view of the underlying bytes for the given key.
   */
  byte[] toBytesNoCopy() {
    return rawKey;
  }

  /**
   * Returns the length of this key in bytes.
   */
  public int size() {
    return rawKey.length;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (this.getClass() == o.getClass()) {
      AbstractKey that = (AbstractKey) o;
      return Arrays.equals(rawKey, that.rawKey);
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return Arrays.hashCode(rawKey);
  }

  @Override
  public String toString() {
    return byteArrayToHex(rawKey);
  }
}
