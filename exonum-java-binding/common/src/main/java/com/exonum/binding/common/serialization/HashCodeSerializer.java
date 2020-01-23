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

import com.exonum.binding.common.hash.HashCode;

enum HashCodeSerializer implements Serializer<HashCode> {
  INSTANCE;

  @Override
  public byte[] toBytes(HashCode value) {
    return value.asBytes();
  }

  @Override
  public HashCode fromBytes(byte[] serializedValue) {
    return HashCode.fromBytes(serializedValue);
  }
}
