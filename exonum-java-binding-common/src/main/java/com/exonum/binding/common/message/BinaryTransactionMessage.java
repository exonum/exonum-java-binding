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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.copyOf;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Binary implementation of the {@link TransactionMessage} class.
 */
public final class BinaryTransactionMessage implements TransactionMessage {
  private final ByteBuffer rawTransaction;

  BinaryTransactionMessage(byte[] bytes) {
    this.rawTransaction = ByteBuffer.wrap(copyOf(bytes, bytes.length)).order(LITTLE_ENDIAN);
  }

  @Override
  public PublicKey getAuthor() {
    byte[] key = new byte[AUTHOR_PUBLIC_KEY_SIZE];
    rawTransaction.position(AUTHOR_PUBLIC_KEY_OFFSET);
    rawTransaction.get(key);
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
    rawTransaction.position(PAYLOAD_OFFSET);
    rawTransaction.get(payload);
    return payload;
  }

  @Override
  public HashCode hash() {
    return sha256().newHasher()
        .putBytes(rawTransaction.duplicate())
        .hash();
  }

  @Override
  public byte[] getSignature() {
    int payloadSize = rawTransaction.limit() - (PAYLOAD_OFFSET + SIGNATURE_SIZE);
    rawTransaction.position(PAYLOAD_OFFSET + payloadSize);
    byte[] signature = new byte[SIGNATURE_SIZE];
    rawTransaction.get(signature);
    return signature;
  }

  @Override
  public byte[] toBytes() {
    byte[] bytes = rawTransaction.array();
    return copyOf(bytes, bytes.length);
  }

  @Override
  public int size() {
    return rawTransaction.limit();
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

  @Override
  public String toString() {
    return Arrays.toString(rawTransaction.array());
  }

}
