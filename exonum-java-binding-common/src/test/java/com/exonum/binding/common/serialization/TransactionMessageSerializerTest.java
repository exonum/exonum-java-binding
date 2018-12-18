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

package com.exonum.binding.common.serialization;

import static com.exonum.binding.common.message.TransactionMessage.AUTHOR_PUBLIC_KEY_SIZE;
import static com.exonum.binding.common.message.TransactionMessage.SIGNATURE_SIZE;
import static com.exonum.binding.common.serialization.StandardSerializersTest.roundTripTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.test.Bytes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TransactionMessageSerializerTest {

  private Serializer<TransactionMessage> serializer = TransactionMessageSerializer.INSTANCE;

  @Test
  void roundTrip() {
    byte[] payload = Bytes.randomBytes(100);
    byte[] publicKey = Bytes.randomBytes(AUTHOR_PUBLIC_KEY_SIZE);
    KeyPair keys = KeyPair.createKeyPair(Bytes.bytes(0x00), publicKey);
    byte[] signature = Bytes.randomBytes(SIGNATURE_SIZE);
    CryptoFunction cryptoFunction = Mockito.mock(CryptoFunction.class);
    when(cryptoFunction.signMessage(any(), eq(keys.getPrivateKey()))).thenReturn(signature);

    TransactionMessage message = TransactionMessage.builder()
        .serviceId((short) 1)
        .transactionId((short) 2)
        .payload(payload)
        .sign(keys, cryptoFunction);

    roundTripTest(message, serializer);
  }

}
