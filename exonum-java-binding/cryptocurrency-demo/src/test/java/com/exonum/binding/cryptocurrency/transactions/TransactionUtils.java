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

package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.cryptocurrency.ByteStrings;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.transaction.RawTransaction;
import com.google.protobuf.ByteString;

/**
 * Helper class to create transaction messages from raw transactions.
 */
public class TransactionUtils {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  static final long DEFAULT_INITIAL_BALANCE = 100L;

  /**
   * Returns a CreateWalletTx transaction message with given default initial balance and signed
   * with given owner key pair.
   */
  static TransactionMessage newCreateWalletTransaction(
      long initialBalance, KeyPair ownerKeyPair) {
    RawTransaction rawTransaction = newCreateWalletRawTransaction(initialBalance);
    return toTransactionMessage(rawTransaction, ownerKeyPair);
  }

  /**
   * Creates new raw create wallet transaction with a given initial balance.
   */
  static RawTransaction newCreateWalletRawTransaction(long initialBalance) {
    return RawTransaction.newBuilder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(CreateWalletTx.ID)
        .payload(TxMessageProtos.CreateWalletTx.newBuilder()
            .setInitialBalance(initialBalance)
            .setName("Jack")
            .build()
            .toByteArray())
        .build();
  }

  /**
   * Returns a TransferTx transaction message with given seed, receiver key and transfer amount and
   * signed with given owner key pair.
   */
  static TransactionMessage newTransferTransaction(
      long seed, KeyPair ownerKeyPair, PublicKey receiverKey, long sum) {
    RawTransaction rawTransaction = newTransferRawTransaction(seed, sum, receiverKey);
    return toTransactionMessage(rawTransaction, ownerKeyPair);
  }

  /**
   * Creates a new raw transfer transaction message using the provided receiver key and amount.
   */
  static RawTransaction newTransferRawTransaction(
      long seed, long amount, PublicKey recipientId) {
    return RawTransaction.newBuilder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(TransferTx.ID)
        .payload(TxMessageProtos.TransferTx.newBuilder()
            .setSeed(seed)
            .setToWallet(fromPublicKey(recipientId))
            .setSum(amount)
            .build()
            .toByteArray())
        .build();
  }

  /**
   * Returns key byte string.
   */
  private static ByteString fromPublicKey(PublicKey k) {
    return ByteStrings.copyFrom(k);
  }

  /**
   * Given a {@code rawTransaction}, signs it using given key pair and returns a resulting
   * transaction message.
   */
  private static TransactionMessage toTransactionMessage(
      RawTransaction rawTransaction, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(rawTransaction.getServiceId())
        .transactionId(rawTransaction.getTransactionId())
        .payload(rawTransaction.getPayload())
        .sign(keyPair, CRYPTO_FUNCTION);
  }

  private TransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
