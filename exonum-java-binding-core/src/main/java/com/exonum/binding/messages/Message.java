/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.messages;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;

/**
 * An Exonum network message.
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
   */
  ByteBuffer getSignature();

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
    }

    @Override
    public Builder setBody(ByteBuffer body) {
      int bodySize = body.remaining();
      checkArgument(bodySize <= MAX_BODY_SIZE, "The body is too big (%s)", bodySize);
      return super.setBody(body.duplicate());
    }

    @Override
    public Builder setSignature(ByteBuffer signature) {
      int signatureSize = signature.remaining();
      checkArgument(signatureSize == SIGNATURE_SIZE, "Invalid signature size (%s)", signatureSize);
      return super.setSignature(signature.duplicate());
    }

    public BinaryMessage buildRaw() {
      Message message = build();
      return BinaryMessageBuilder.toBinary(message);
    }
  }
}
