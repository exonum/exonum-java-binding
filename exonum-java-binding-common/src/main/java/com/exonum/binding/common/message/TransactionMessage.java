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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An Exonum transaction message.
 */
public interface TransactionMessage {
  int AUTHOR_PUBLIC_KEY_OFFSET = 0;
  int CLS_OFFSET = 32;
  int TAG_OFFSET = 33;
  int SERVICE_ID_OFFSET = 34;
  int TRANSACTION_ID_OFFSET = 36;
  int PAYLOAD_OFFSET = 38;

  int AUTHOR_PUBLIC_KEY_SIZE = 32;
  int SIGNATURE_SIZE = 64;

  /**
   * Returns a public key of the Transaction Message's author.
   */
  PublicKey getAuthor();

  /**
   * Returns service id of the Transaction Message.
   */
  short getServiceId();

  /**
   * Returns transaction id of the Transaction Message.
   */
  short getTransactionId();

  /**
   * Returns payload of the Transaction Message.
   */
  byte[] getPayload();

  /**
   * Returns Transaction Message hash.
   */
  HashCode hash();

  /**
   * Returns Transaction Message signature.
   */
  byte[] getSignature();

  /**
   * Returns Transaction Message in binary format.
   */
  byte[] toBytes();

  /**
   * Returns the Builder for the Transaction Message.
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * Creates Transaction Message from the given bytes array.
   */
  static TransactionMessage fromBytes(byte[] bytes) {
    return BinaryTransactionMessage.fromBuffer(ByteBuffer.wrap(bytes));
  }

  /**
   * Creates Transaction Message from the given bytes buffer.
   */
  static TransactionMessage fromBuffer(ByteBuffer buffer) {
    return BinaryTransactionMessage.fromBuffer(buffer);
  }

  /**
   * Transaction Message Builder class.
   */
  class Builder {
    private Short serviceId;
    private Short transactionId;
    private ByteBuffer payload;

    public Builder serviceId(short serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder transactionId(short transactionId) {
      this.transactionId = transactionId;
      return this;
    }

    public Builder payload(byte[] payload) {
      return payload(ByteBuffer.wrap(payload));
    }

    public Builder payload(ByteBuffer payload) {
      this.payload = payload.duplicate().order(ByteOrder.LITTLE_ENDIAN);
      return this;
    }

    public TransactionMessage sign(KeyPair keys, CryptoFunction crypto) {
      checkNotNull(serviceId);
      checkNotNull(transactionId);
      checkNotNull(payload);
      PublicKey authorPublicKey = keys.getPublicKey();
      checkArgument(authorPublicKey.size() == AUTHOR_PUBLIC_KEY_SIZE);

      ByteBuffer buffer = ByteBuffer
          .allocate(PAYLOAD_OFFSET + payload.limit() + SIGNATURE_SIZE)
          .order(ByteOrder.LITTLE_ENDIAN);
      buffer.put(authorPublicKey.toBytes());
      buffer.put(MessageType.TRANSACTION.bytes());
      buffer.putShort(serviceId);
      buffer.putShort(transactionId);
      buffer.put(payload);

      buffer.position(0);
      byte[] unsignedMessage = new byte[PAYLOAD_OFFSET + payload.limit()];
      buffer.get(unsignedMessage);
      byte[] signature = crypto.signMessage(unsignedMessage, keys.getPrivateKey());
      buffer.put(signature);

      return new BinaryTransactionMessage(buffer);
    }

    private Builder() {
    }
  }

}
