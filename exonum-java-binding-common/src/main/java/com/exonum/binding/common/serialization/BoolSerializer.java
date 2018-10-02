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
 *
 */

package com.exonum.binding.common.serialization;

import static com.google.common.base.Preconditions.checkArgument;

enum BoolSerializer implements Serializer<Boolean> {
  INSTANCE;

  private static final int BOOLEAN_BYTES = 1;
  private static final byte BOOLEAN_TRUE = 1;
  private static final byte BOOLEAN_FALSE = 0;

  @Override
  public byte[] toBytes(Boolean value) {
    byte b = value ? BOOLEAN_TRUE : BOOLEAN_FALSE;
    return new byte[]{b};
  }

  @Override
  public Boolean fromBytes(byte[] serializedValue) {
    checkArgument(serializedValue.length == BOOLEAN_BYTES,
        "Expected an array of size %s, but was %s", BOOLEAN_BYTES, serializedValue.length);
    byte value = serializedValue[0];
    checkArgument(isValidBoolean(value), "Is not a boolean value");

    return value == BOOLEAN_TRUE;
  }

  private static boolean isValidBoolean(byte value) {
    return value == BOOLEAN_FALSE || value == BOOLEAN_TRUE;
  }

}
