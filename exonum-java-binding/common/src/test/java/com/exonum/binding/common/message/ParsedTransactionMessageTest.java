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

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunctions.Ed25519;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.core.messages.Consensus;
import com.exonum.core.messages.Consensus.ExonumMessage;
import com.exonum.core.messages.Consensus.Prevote;
import com.exonum.core.messages.Messages;
import com.exonum.core.messages.Runtime.AnyTx;
import com.exonum.core.messages.Runtime.CallInfo;
import com.exonum.core.messages.Types;
import com.exonum.core.messages.Types.Signature;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ParsedTransactionMessageTest {

  @Nested
  class WithSignedMessage {
    final int serviceId = 1;
    final int transactionId = 2;
    final ByteString txArguments = ByteString.copyFrom(new byte[]{1, 2, 3});
    final ByteString signature = ByteString.copyFrom(new byte[Ed25519.SIGNATURE_BYTES]);
    final PublicKey authorPublicKey = PublicKey.fromHexString("abcd");

    Messages.SignedMessage signedMessage;

    @BeforeEach
    void createSignedMessage() {
      byte[] exonumMessage = ExonumMessage.newBuilder()
          .setAnyTx(AnyTx.newBuilder()
              .setCallInfo(CallInfo.newBuilder()
                  .setInstanceId(serviceId)
                  .setMethodId(transactionId)
                  .build())
              .setArguments(txArguments)
              .build())
          .build()
          .toByteArray();

      signedMessage = aSignedMessageProto()
          .setAuthor(Types.PublicKey.newBuilder()
              .setData(ByteString.copyFrom(authorPublicKey.toBytes()))
              .build())
          .setPayload(ByteString.copyFrom(exonumMessage))
          .setSignature(Signature.newBuilder()
              .setData(signature)
              .build())
          .build();
    }

    @Test
    void parseSignedMessage() {
      // Parse the signed message
      ParsedTransactionMessage message = new ParsedTransactionMessage(signedMessage);

      // Verify all the properties are correct
      assertThat(message.getServiceId()).isEqualTo(serviceId);
      assertThat(message.getTransactionId()).isEqualTo(transactionId);
      assertThat(message.getPayload()).isEqualTo(txArguments);
      assertThat(message.getAuthor()).isEqualTo(authorPublicKey);
      assertThat(message.getSignature()).isEqualTo(signature.toByteArray());
    }

    @Test
    void toBytesRoundtrip() throws InvalidProtocolBufferException {
      ParsedTransactionMessage message = new ParsedTransactionMessage(signedMessage);

      // Serialize the message
      byte[] serializedMessage = message.toBytes();

      // Decode as the protobuf message
      Messages.SignedMessage signedProtoFromBytes =
          Messages.SignedMessage.parseFrom(serializedMessage);

      // Check it is equal to the original proto message
      assertThat(signedProtoFromBytes).isEqualTo(signedMessage);

      // Check the parsed message will also be equal
      ParsedTransactionMessage parsedFromBytes = new ParsedTransactionMessage(signedProtoFromBytes);
      assertThat(parsedFromBytes).isEqualTo(message);
    }
  }

  @Test
  void createMessageNotTx() {
    // Use Prevote message instead of AnyTx
    byte[] prevoteMessage = Consensus.ExonumMessage.newBuilder()
        .setPrevote(Prevote.getDefaultInstance())
        .build()
        .toByteArray();

    Messages.SignedMessage signedMessage = aSignedMessageProto()
        .setPayload(ByteString.copyFrom(prevoteMessage))
        .build();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new ParsedTransactionMessage(signedMessage));

    assertThat(e.getMessage())
        .containsIgnoringCase("does not contain a transaction")
        .containsIgnoringCase("Prevote");
  }

  @Test
  void testEquals() throws InvalidProtocolBufferException {
    String red = "Red";
    String black = "Black";
    EqualsVerifier.forClass(ParsedTransactionMessage.class)
        // Fields are non-null
        .suppress(Warning.NULL_FIELDS)
        // Only the source signedMessage is compared
        .withOnlyTheseFields("signedMessage")
        .withPrefabValues(Messages.SignedMessage.class,
            signedConsensusMessage(red),
            signedConsensusMessage(black))
        .withPrefabValues(SignedMessage.class,
            signedMessage(red),
            signedMessage(black))
        .withPrefabValues(AnyTx.class,
            anyTx(red),
            anyTx(black))
        .verify();
  }

  private static SignedMessage signedMessage(String payload) throws InvalidProtocolBufferException {
    return SignedMessage.fromProto(signedConsensusMessage(payload));
  }

  private static Messages.SignedMessage signedConsensusMessage(String payload) {
    return aSignedMessageProto()
        .setPayload(ExonumMessage.newBuilder()
            .setAnyTx(anyTx(payload))
            .build()
            .toByteString())
        .build();
  }

  private static Messages.SignedMessage.Builder aSignedMessageProto() {
    return Messages.SignedMessage.newBuilder()
        // Set the author only and keep the rest as defaults as the parser requires a non-empty key
        .setAuthor(Types.PublicKey.newBuilder()
            .setData(ByteString.copyFrom(bytes(1, 2, 3, 4)))
            .build());
  }

  private static AnyTx anyTx(String payload) {
    return AnyTx.newBuilder()
        .setArguments(ByteString.copyFrom(bytes(payload)))
        .build();
  }
}
