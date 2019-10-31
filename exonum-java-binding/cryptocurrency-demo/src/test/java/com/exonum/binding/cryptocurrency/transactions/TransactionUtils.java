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
import com.google.protobuf.ByteString;

/**
 * Helper class to create transaction messages from raw transactions.
 */
public final class TransactionUtils {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  static final long DEFAULT_INITIAL_BALANCE = 100L;

  /**
   * Returns a CreateWalletTx transaction message with given default initial balance and
   * service id and signed with given owner key pair.
   */
  static TransactionMessage newCreateWalletTransaction(
      long initialBalance, KeyPair ownerKeyPair, int serviceId) {
    return TransactionMessage.builder()
        .payload(createCreateWalletTxPayload(initialBalance))
        .serviceId(serviceId)
        .transactionId(CreateWalletTx.ID)
        .sign(ownerKeyPair, CRYPTO_FUNCTION);
  }

  /**
   * Create a CreateWalletTx transaction payload with given initial balance.
   */
  static byte[] createCreateWalletTxPayload(long initialBalance) {
    return TxMessageProtos.CreateWalletTx.newBuilder()
        .setInitialBalance(initialBalance)
        .build()
        .toByteArray();
  }

  /**
   * Returns a TransferTx transaction message with given seed, receiver key, transfer amount and
   * service id and signed with given owner key pair.
   */
  static TransactionMessage newTransferTransaction(
      long seed, KeyPair ownerKeyPair, PublicKey receiverKey, long sum, int serviceId) {
    return TransactionMessage.builder()
        .payload(createTransferTxPayload(seed, receiverKey, sum))
        .serviceId(serviceId)
        .transactionId(CreateWalletTx.ID)
        .sign(ownerKeyPair, CRYPTO_FUNCTION);
  }

  /**
   * Create a TransferTx transaction payload with given seed, receiver key and sum.
   */
  static byte[] createTransferTxPayload(long seed, PublicKey receiverKey, long sum) {
    return TxMessageProtos.TransferTx.newBuilder()
        .setSeed(seed)
        .setToWallet(fromPublicKey(receiverKey))
        .setSum(sum)
        .build()
        .toByteArray();
  }

  /**
   * Returns key byte string.
   */
  private static ByteString fromPublicKey(PublicKey k) {
    return ByteString.copyFrom(k.toBytes());
  }

  private TransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
