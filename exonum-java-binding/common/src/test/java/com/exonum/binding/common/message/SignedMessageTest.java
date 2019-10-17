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

import static com.exonum.binding.common.crypto.AbstractKey.keyFunnel;
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.CryptoFunctions.Ed25519;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.messages.Consensus;
import com.exonum.binding.messages.Consensus.ExonumMessage;
import com.exonum.binding.messages.Types;
import com.exonum.binding.messages.Types.Signature;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SignedMessageTest {

  private static KeyPair TEST_KEY_PAIR = CryptoFunctions.ed25519().generateKeyPair();
  private static PublicKey TEST_PUBLIC_KEY = TEST_KEY_PAIR.getPublicKey();

  @Nested
  class ParsedFromTestData {
    private ExonumMessage payload;
    private byte[] testSignature;
    private Consensus.SignedMessage message;

    @BeforeEach
    void createProtoMessage() {
      payload = ExonumMessage.getDefaultInstance();
      Types.PublicKey authorPk = aPublicKey()
          .setData(ByteString.copyFrom(TEST_PUBLIC_KEY.toBytes()))
          .build();
      testSignature = new byte[Ed25519.SIGNATURE_BYTES];
      Types.Signature signature = aSignature()
          .setData(ByteString.copyFrom(testSignature))
          .build();
      message = Consensus.SignedMessage.newBuilder()
          .setPayload(payload.toByteString())
          .setAuthor(authorPk)
          .setSignature(signature)
          .build();
    }

    @Test
    void parseFrom() throws InvalidProtocolBufferException {
      // Check it can be parsed
      SignedMessage signedMessage = SignedMessage.parseFrom(message.toByteArray());

      // Verify it includes correct data
      assertThat(signedMessage.getPayload()).isEqualTo(payload);
      assertThat(signedMessage.getAuthorPk()).isEqualTo(TEST_PUBLIC_KEY);
      assertThat(signedMessage.getSignature()).isEqualTo(testSignature);
    }

    @Test
    void hash() throws InvalidProtocolBufferException {
      SignedMessage signedMessage = SignedMessage.fromProto(message);

      HashCode hash = signedMessage.hash();

      // Hash the source objects used to construct the message in the required order
      HashCode expectedHash = sha256().newHasher()
          .putBytes(payload.toByteArray())
          .putObject(TEST_PUBLIC_KEY, keyFunnel())
          .putBytes(testSignature)
          .hash();

      assertThat(hash).isEqualTo(expectedHash);
    }
  }

  @Test
  void parseFromWrongMessage() {
    byte[] message = bytes("Not a signed message");

    // Check it cannot be parsed
    assertThrows(InvalidProtocolBufferException.class, () -> SignedMessage.parseFrom(message));
  }

  @Test
  void parseFromPayloadNotExonumMessage() {
    ByteString invalidPayload = ByteString.copyFrom(
        bytes("Invalid payload: not an ExonumMessage"));
    Types.PublicKey authorPk = aPublicKey().build();
    Types.Signature signature = aSignature().build();
    byte[] message = Consensus.SignedMessage.newBuilder()
        .setPayload(invalidPayload)
        .setAuthor(authorPk)
        .setSignature(signature)
        .build()
        .toByteArray();

    // Check it cannot be parsed
    assertThrows(InvalidProtocolBufferException.class, () -> SignedMessage.parseFrom(message));
  }

  private static Types.PublicKey.Builder aPublicKey() {
    return Types.PublicKey.newBuilder()
        .setData(ByteString.copyFrom(TEST_PUBLIC_KEY.toBytes()));
  }

  private static Types.Signature.Builder aSignature() {
    byte[] testSignature = new byte[Ed25519.SIGNATURE_BYTES];
    return Signature.newBuilder()
        .setData(ByteString.copyFrom(testSignature));
  }
}
