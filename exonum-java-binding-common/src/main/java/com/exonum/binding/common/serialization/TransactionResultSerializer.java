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

package com.exonum.binding.common.serialization;

import com.exonum.binding.common.blockchain.TransactionResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public enum TransactionResultSerializer implements Serializer<TransactionResult> {
  INSTANCE;

  @Override
  public byte[] toBytes(TransactionResult value) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutput out = new ObjectOutputStream(bos)) {
      out.writeObject(value);
      return bos.toByteArray();
    } catch (IOException e) {
      throw new AssertionError("Couldn't serialize value " + value, e);
    }
  }

  @Override
  public TransactionResult fromBytes(byte[] serializedValue) {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(serializedValue);
         ObjectInput in = new ObjectInputStream(bis)) {
      return (TransactionResult) in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalArgumentException(
          "Serialized value has wrong format " + Arrays.toString(serializedValue));
    }
  }

}
