/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.common.message;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.messages.consensus.Consensus.ExonumMessage;
import com.exonum.messages.core.Messages;
import com.exonum.messages.core.runtime.Base.AnyTx;
import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

/**
 * A parsed transaction message. On instantiation, it decodes the signed message payload
 * as an Exonum transaction.
 */
final class ParsedTransactionMessage implements TransactionMessage {

  // signedMessage is the source protocol buffers message; parsedMessage is its parsed
  // representation.
  private final Messages.SignedMessage signedMessage;
  private final SignedMessage parsedMessage;
  private final AnyTx tx;

  /**
   * Creates a transaction message from the signed message.
   * @param signedMessage a signed exonum transaction message
   * @throws IllegalArgumentException if the signed message does not contain an Exonum transaction
   *     message
   */
  ParsedTransactionMessage(Messages.SignedMessage signedMessage) {
    this.signedMessage = signedMessage;
    // Decode the signed message
    try {
      this.parsedMessage = SignedMessage.fromProto(signedMessage);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e);
    }

    // Decode the transaction
    ExonumMessage payload = parsedMessage.getPayload();
    checkArgument(payload.hasAnyTx(), "SignedMessage does not contain a transaction "
        + "in its payload but %s", payload.getKindCase());

    this.tx = payload.getAnyTx();
  }

  @Override
  public PublicKey getAuthor() {
    return parsedMessage.getAuthorPk();
  }

  @Override
  public int getServiceId() {
    return tx.getCallInfo().getInstanceId();
  }

  @Override
  public int getTransactionId() {
    return tx.getCallInfo().getMethodId();
  }

  @Override
  public ByteString getPayload() {
    return tx.getArguments();
  }

  @Override
  public HashCode hash() {
    return parsedMessage.hash();
  }

  @Override
  public byte[] getSignature() {
    return parsedMessage.getSignature();
  }

  @Override
  public byte[] toBytes() {
    return signedMessage.toByteArray();
  }

  @Override
  public String toString() {
    // Include only the fields that identify this message and allow to re-create it.
    // It is assumed that if someone needs it in its exact, binary, form, they can use #toBytes
    // instead.
    return MoreObjects.toStringHelper("TransactionMessage")
        .add("author", getAuthor())
        .add("serviceId", getServiceId())
        .add("transactionId", getTransactionId())
        .add("payload", getPayload())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParsedTransactionMessage)) {
      return false;
    }
    ParsedTransactionMessage that = (ParsedTransactionMessage) o;
    // We compare only signedMessage, as all the other fields are derived from it
    return signedMessage.equals(that.signedMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(signedMessage);
  }
}

