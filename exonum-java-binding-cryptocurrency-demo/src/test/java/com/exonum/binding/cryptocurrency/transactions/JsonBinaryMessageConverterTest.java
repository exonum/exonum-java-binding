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

package com.exonum.binding.cryptocurrency.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.test.Bytes;
import com.google.common.io.BaseEncoding;
import com.google.gson.JsonParseException;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JsonBinaryMessageConverterTest {

  private static final byte[] SIGNATURE = Bytes.createPrefixed(Bytes.bytes(0xAB),
      Message.SIGNATURE_SIZE);
  private static final String SIGNATURE_HEX = BaseEncoding.base16().lowerCase().encode(SIGNATURE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final JsonBinaryMessageConverter converter = new JsonBinaryMessageConverter();

  @Test
  public void convertCreateWalletMessage() throws InvalidProtocolBufferException {
    String json = "{ "
        + "\"protocol_version\": 0, "
        + "\"service_id\": 42, "
        + "\"message_id\": 1, "
        + "\"body\": {"
        + "  \"ownerPublicKey\": \"ab\", "
        + "  \"initialBalance\": \"100\""
        + "}, "
        + "\"signature\": \"" + SIGNATURE_HEX + "\""
        + " }";

    BinaryMessage message = converter.toMessage(json);

    assertThat(message.getNetworkId()).isZero();
    assertThat(message.getVersion()).isEqualTo((byte) 0);
    assertThat(message.getServiceId()).isEqualTo((short) 42);
    assertThat(message.getMessageType()).isEqualTo((short) 1);
    assertThat(message.getSignature()).inHexadecimal()
        .isEqualTo(SIGNATURE);

    TxMessagesProtos.CreateWalletTx txData = TxMessagesProtos.CreateWalletTx.parseFrom(
        message.getBody());

    assertThat(txData.getOwnerPublicKey())
        .isEqualTo(ByteString.copyFrom(Bytes.fromHex("ab")));
    assertThat(txData.getInitialBalance()).isEqualTo(100);
  }

  @Test
  public void convertTransferMessage() throws InvalidProtocolBufferException {
    String json = "{ "
        + "\"protocol_version\": 0, "
        + "\"service_id\": 42, "
        + "\"message_id\": 2, "
        + "\"body\": {"
        + "  \"seed\": 1, "
        + "  \"senderId\": \"0a\", "
        + "  \"recipientId\": \"0b\", "
        + "  \"amount\": \"50\""
        + "}, "
        + "\"signature\": \"" + SIGNATURE_HEX + "\""
        + " }";

    BinaryMessage message = converter.toMessage(json);

    assertThat(message.getNetworkId()).isZero();
    assertThat(message.getVersion()).isEqualTo((byte) 0);
    assertThat(message.getServiceId()).isEqualTo((short) 42);
    assertThat(message.getMessageType()).isEqualTo((short) 2);
    assertThat(message.getSignature()).inHexadecimal()
        .isEqualTo(SIGNATURE);

    TxMessagesProtos.TransferTx txData = TxMessagesProtos.TransferTx.parseFrom(
        message.getBody());

    assertThat(txData.getSeed()).isEqualTo(1);
    assertThat(txData.getFromWallet())
        .isEqualTo(ByteString.copyFrom(Bytes.fromHex("0a")));
    assertThat(txData.getToWallet())
        .isEqualTo(ByteString.copyFrom(Bytes.fromHex("0b")));
    assertThat(txData.getSum()).isEqualTo(50);
  }

  @Test
  public void convertIllegalJson() {
    String json = "{ fooBar";

    expectedException.expect(JsonParseException.class);
    converter.toMessage(json);
  }

  @Test
  public void convertNotMessage() {
    String json = "{ \"foo\": \"bar\" }";

    expectedException.expect(IllegalArgumentException.class);
    converter.toMessage(json);
  }

  @Test
  public void convertUnknownServiceId() {
    // Transaction of unknown service disguised as "create wallet" message.
    String json = "{ "
        + "\"protocol_version\": 0, "
        + "\"service_id\": 1024, "
        + "\"message_id\": 1, "
        + "\"body\": {"
        + "  \"ownerPublicKey\": \"ab\", "
        + "  \"initialBalance\": \"100\""
        + "}, "
        + "\"signature\": \"" + SIGNATURE_HEX + "\""
        + " }";

    expectedException.expect(IllegalArgumentException.class);
    converter.toMessage(json);
  }

  @Test
  public void convertUnknownMessage() {
    String json = "{ "
        + "\"protocol_version\": 0, "
        + "\"service_id\": 42, "
        + "\"message_id\": 1024, "
        + "\"body\": {"
        + "  \"foo\": 1"
        + "}, "
        + "\"signature\": \"" + SIGNATURE_HEX + "\""
        + " }";

    expectedException.expect(IllegalArgumentException.class);
    converter.toMessage(json);
  }
}
