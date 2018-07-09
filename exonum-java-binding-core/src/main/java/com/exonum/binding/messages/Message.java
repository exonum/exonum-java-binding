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

package com.exonum.binding.messages;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.crypto.CryptoFunctions;
import java.nio.ByteBuffer;

/**
 * An Exonum network message.
 *
 * <p>The message hierarchy does not work well with the target use-cases,
 * e.g., you cannot do much without a BinaryMessage, including signing,
 * see ECR-745 for details. However, improvements are delayed so that
 * they are made to the new message format that is going to be integrated soon
 * in the core framework. Until then, use something like that to sign a message:
 * <pre>
 * {@code
 *
 * BinaryMessage signedMessage = new Message.Builder()
 *   // (0) Set all required fields, setting signature is not required.
 *   //…
 *   // (1) Create an unsigned message.
 *   .buildRaw()
 *   // (2) Sign it using BinaryMessage#sign.
 *   .sign(CryptoFunctions.ed25519(), authorSecretKey);
 * }
 * </pre>
 */
// @FreeBuilder()
public interface Message {
  int NET_ID_OFFSET = 0;
  int VERSION_OFFSET = 1;
  int MESSAGE_TYPE_OFFSET = 2;
  int SERVICE_ID_OFFSET = 4;
  int PAYLOAD_LENGTH_OFFSET = 6;
  int HEADER_SIZE = 10;
  int BODY_OFFSET = HEADER_SIZE;
  int SIGNATURE_SIZE = 64;
  int MAX_BODY_SIZE = Integer.MAX_VALUE - (HEADER_SIZE + SIGNATURE_SIZE);

  /**
   * Calculates the message size given the size of the body.
   *
   * @param bodySize the size of the message body in bytes
   * @return the size of the whole message in bytes
   * @throws IllegalArgumentException if bodySize is negative
   */
  static int messageSize(int bodySize) {
    checkArgument(0 <= bodySize && bodySize <= MAX_BODY_SIZE,
        "Body size (%s) is invalid", bodySize);
    return HEADER_SIZE + bodySize + SIGNATURE_SIZE;
  }

  /**
   * Returns the blockchain network id.
   */
  byte getNetworkId();

  /**
   * Returns the major version of the Exonum serialization protocol.
   */
  byte getVersion();

  /**
   * Returns the identifier of the service this message belongs to,
   * or zero if this message is an internal Exonum message.
   */
  short getServiceId();

  /**
   * Returns the type of this message within a service (e.g., a transaction identifier).
   */
  short getMessageType();

  /**
   * Returns the message body.
   */
  ByteBuffer getBody();

  /**
   * Returns the <a href="https://ed25519.cr.yp.to/">Ed25519</a> signature
   * over this binary message.
   *
   * <p>The signature is <strong>not</strong> guaranteed to be valid and must be verified against
   * the signer’s public key.
   *
   * @see CryptoFunctions#ed25519()
   */
  byte[] getSignature();

  /**
   * Returns the signature offset in this message.
   */
  default int signatureOffset() {
    return size() - SIGNATURE_SIZE;
  }

  /**
   * Returns the size of a binary representation of this message in bytes.
   */
  default int size() {
    return messageSize(getBody().remaining());
  }

  class Builder extends Message_Builder2 {

    private static final byte DEFAULT_NETWORK_ID = 0;
    private static final byte DEFAULT_PROTOCOL_VERSION = 0;

    public Builder() {
      // Set defaults. Currently the only valid values of network and protocol version are 0.
      setNetworkId(DEFAULT_NETWORK_ID);
      setVersion(DEFAULT_PROTOCOL_VERSION);
      // Set an empty signature so that the clients do not *have* to.
      setSignature(new byte[SIGNATURE_SIZE]);
    }

    @Override
    public Builder setBody(ByteBuffer body) {
      int bodySize = body.remaining();
      checkArgument(bodySize <= MAX_BODY_SIZE, "The body is too big (%s)", bodySize);
      return super.setBody(body.duplicate());
    }

    @Override
    public Builder setSignature(byte[] signature) {
      int signatureSize = signature.length;
      checkArgument(signatureSize == SIGNATURE_SIZE, "Invalid signature size (%s)", signatureSize);
      return super.setSignature(signature);
    }

    public BinaryMessage buildRaw() {
      Message message = build();
      return BinaryMessageBuilder.toBinary(message);
    }
  }
}
