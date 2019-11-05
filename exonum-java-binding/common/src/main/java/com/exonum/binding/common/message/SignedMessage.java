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

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.messages.Consensus;
import com.exonum.binding.messages.Consensus.ExonumMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A wrapper around {@link Consensus.SignedMessage} protobuf message containing
 * {@link Consensus.ExonumMessage}, which converts protobuf types into internal types.
 *
 * <p>It currently does not support verification of the signature against the author's public
 * key — such functionality may be added later if needed.
 */
final class SignedMessage {

  private final ExonumMessage payload;
  private final PublicKey authorPk;
  private final ByteString signature;
  private final HashCode hash;

  private SignedMessage(ExonumMessage payload, PublicKey authorPk,
                        ByteString signature, HashCode hash) {
    this.payload = payload;
    this.authorPk = authorPk;
    this.signature = signature;
    this.hash = hash;
  }

  /**
   * Parses the signed message bytes. The parsing does not involve the signature verification —
   * do it separately if needed.
   *
   * @param messageBytes the serialized message to parse
   * @return a signed message with exonum message as its payload
   * @throws InvalidProtocolBufferException if the given bytes are not a serialized
   *     {@link Consensus.SignedMessage}; or if the payload of the message is not
   *     {@link Consensus.ExonumMessage}
   */
  static SignedMessage parseFrom(byte[] messageBytes) throws InvalidProtocolBufferException {
    // Try to decode the SignedMessage container
    Consensus.SignedMessage message = Consensus.SignedMessage.parseFrom(messageBytes);
    return fromProto(message);
  }

  /**
   * Creates a wrapper around signed message.
   *
   * @param message a signed message
   * @throws InvalidProtocolBufferException if a signed message does not contain a valid payload
   *     that is a serialized {@link Consensus.ExonumMessage}
   */
  static SignedMessage fromProto(Consensus.SignedMessage message)
      throws InvalidProtocolBufferException {
    // Try to decode the payload, which is stored as bytes. It is expected to be an ExonumMessage
    ByteString payloadBytes = message.getPayload();
    ExonumMessage payload = ExonumMessage.parseFrom(payloadBytes);

    PublicKey authorPk = PublicKey.fromBytes(message.getAuthor()
        .getData()
        .toByteArray());
    ByteString signature = message.getSignature().getData();

    HashCode hash = Hashing.sha256().hashBytes(message.toByteArray());

    return new SignedMessage(payload, authorPk, signature, hash);
  }

  /**
   * Returns the message payload.
   */
  Consensus.ExonumMessage getPayload() {
    return payload;
  }

  /**
   * Returns the key of the message author.
   *
   * <p>The correctness of the signature is <strong>not</strong> verified against this key
   * and must be done separately if needed.
   */
  PublicKey getAuthorPk() {
    return authorPk;
  }

  /**
   * Returns the signature of the payload, created with the private key, corresponding
   * to the author's {@linkplain #getAuthorPk() key}.
   *
   * <p>The correctness of the signature is <strong>not</strong> verified against this key
   * and must be done separately if needed.
   */
  byte[] getSignature() {
    return signature.toByteArray();
  }

  /**
   * Returns the hash of the signed message, which is the hash of the protobuf-serialized
   * representation.
   */
  HashCode hash() {
    return hash;
  }
}
