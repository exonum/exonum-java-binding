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

package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.CryptocurrencyServiceImpl.CRYPTO_FUNCTION;
import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;

import com.exonum.binding.crypto.KeyPair;
import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.google.protobuf.ByteString;

class CreateWalletTransactionUtils {

  static final long DEFAULT_BALANCE = 100L;

  /**
   * Creates new signed binary create wallet message using provided owner key pair.
   */
  static BinaryMessage createSignedMessage(KeyPair ownerKeyPair) {
    BinaryMessage unsignedMessage = createUnsignedMessage(ownerKeyPair.getPublicKey(),
        DEFAULT_BALANCE);
    return unsignedMessage.sign(CRYPTO_FUNCTION, ownerKeyPair.getPrivateKey());
  }

  /**
   * Creates new unsigned binary create wallet message using provided owner key and default balance.
   */
  static BinaryMessage createUnsignedMessage(PublicKey ownerKey) {
    return createUnsignedMessage(ownerKey, DEFAULT_BALANCE);
  }

  /**
   * Creates new unsigned binary create wallet message using provided owner key and provided
   * balance.
   */
  static BinaryMessage createUnsignedMessage(PublicKey ownerKey, long initialBalance) {
    return newCryptocurrencyTransactionBuilder(CreateWalletTx.ID)
        .setBody(TxMessagesProtos.CreateWalletTx.newBuilder()
            .setOwnerPublicKey(ByteString.copyFrom(ownerKey.toBytes()))
            .setInitialBalance(initialBalance)
            .build()
            .toByteArray())
        .setSignature(new byte[Message.SIGNATURE_SIZE])
        .buildRaw();
  }

  private CreateWalletTransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
