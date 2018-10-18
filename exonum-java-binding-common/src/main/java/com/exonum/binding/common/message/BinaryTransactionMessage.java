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

package com.exonum.binding.common.message;

import static com.exonum.binding.common.hash.Hashing.defaultHashFunction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Binary implementation of the {@link TransactionMessage} class.
 */
public final class BinaryTransactionMessage implements TransactionMessage {
  private final ByteBuffer rawTransaction;

  private BinaryTransactionMessage(ByteBuffer rawTransaction) {
    this.rawTransaction = rawTransaction.duplicate().order(ByteOrder.LITTLE_ENDIAN);
    this.rawTransaction.position(0);
  }

  @Override
  public PublicKey getAuthor() {
    byte[] key = new byte[AUTHOR_PUBLIC_KEY_SIZE];
    rawTransaction.get(key, AUTHOR_PUBLIC_KEY_OFFSET, AUTHOR_PUBLIC_KEY_SIZE);
    return PublicKey.fromBytes(key);
  }

  @Override
  public short getServiceId() {
    return rawTransaction.getShort(SERVICE_ID_OFFSET);
  }

  @Override
  public short getTransactionId() {
    return rawTransaction.getShort(TRANSACTION_ID_OFFSET);
  }

  @Override
  public byte[] getPayload() {
    int payloadSize = rawTransaction.limit() - (PAYLOAD_OFFSET + SIGNATURE_SIZE);
    byte[] payload = new byte[payloadSize];
    rawTransaction.get(payload, PAYLOAD_OFFSET, payloadSize);
    return payload;
  }

  @Override
  public HashCode hash() {
    return defaultHashFunction().hashBytes(rawTransaction.array());
  }

  @Override
  public byte[] getSignature() {
    byte[] signature = new byte[SIGNATURE_SIZE];
    int payloadSize = rawTransaction.limit() - (PAYLOAD_OFFSET + SIGNATURE_SIZE);
    int offset = PAYLOAD_OFFSET + payloadSize;
    rawTransaction.get(signature, offset, SIGNATURE_SIZE);
    return signature;
  }

  @Override
  public byte[] toBytes() {
    return rawTransaction.duplicate().array();
  }

  public static BinaryTransactionMessage fromBuffer(ByteBuffer buffer) {
    return new BinaryTransactionMessage(buffer);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BinaryTransactionMessage that = (BinaryTransactionMessage) o;
    return Objects.equal(rawTransaction, that.rawTransaction);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rawTransaction);
  }
}
