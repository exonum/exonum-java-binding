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

package com.exonum.binding.common.message;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.PrivateKey;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.google.errorprone.annotations.CheckReturnValue;
import java.nio.ByteBuffer;

/**
 * A binary Exonum message.
 */
public interface BinaryMessage extends Message {

  /**
   * Creates a binary message from a byte array.
   *
   * @param messageBytes an array with message bytes
   * @return a binary message
   * @throws IllegalArgumentException if message has invalid size
   */
  static BinaryMessage fromBytes(byte[] messageBytes) {
    ByteBuffer buf = ByteBuffer.wrap(messageBytes);
    return MessageReader.wrap(buf);
  }

  /**
   * Returns a message without signature, i.e., without the last 64 bytes of the binary message.
   */
  default byte[] getMessageNoSignature() {
    ByteBuffer signedMessage = getSignedMessage();
    int fullSize = signedMessage.remaining();
    int messageSize = fullSize - Message.SIGNATURE_SIZE;
    byte[] message = new byte[messageSize];
    signedMessage.get(message);
    return message;
  }

  /**
   * Signs this message, creating a new signed binary message.
   *
   * @param cryptoFunction a cryptographic function to use
   * @param authorSecretKey a secret key of the author of this message
   * @throws IllegalArgumentException if the key is not valid for the cryptographic function
   * @return a new signed message
   */
  @CheckReturnValue
  default BinaryMessage sign(CryptoFunction cryptoFunction, PrivateKey authorSecretKey) {
    BinaryMessage unsignedPacket = this;

    byte[] message = unsignedPacket.getMessageNoSignature();
    byte[] signature = cryptoFunction.signMessage(message, authorSecretKey);

    return new Message.Builder()
        .mergeFrom(unsignedPacket)
        .setSignature(signature)
        .buildRaw();
  }

  /**
   * Verifies the cryptographic signature against the given public key.
   *
   * @param cryptoFunction a cryptographic function to use
   * @param authorPublicKey a public key of the author of this message
   * @return true if the transaction is valid; false — otherwise
   */
  @CheckReturnValue
  default boolean verify(CryptoFunction cryptoFunction, PublicKey authorPublicKey) {
    byte[] message = getMessageNoSignature();
    byte[] signature = getSignature();

    return cryptoFunction.verify(message, signature, authorPublicKey);
  }

  /**
   * Returns the whole binary message. It includes a message header, body and signature.
   */
  ByteBuffer getSignedMessage();

  /**
   * Returns the SHA-256 hash of this message.
   */
  default HashCode hash() {
    HashFunction hashFunction = Hashing.defaultHashFunction();
    return hashFunction.hashBytes(getSignedMessage());
  }
}
