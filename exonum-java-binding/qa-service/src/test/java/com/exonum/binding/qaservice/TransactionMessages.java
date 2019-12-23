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

package com.exonum.binding.qaservice;

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.qaservice.QaServiceImpl.UNKNOWN_TX_ID;
import static com.exonum.binding.qaservice.QaTransaction.CREATE_COUNTER;
import static com.exonum.binding.qaservice.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.QaTransaction.VALID_ERROR;
import static com.exonum.binding.qaservice.QaTransaction.VALID_THROWING;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.CreateCounterTxBody;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ErrorTxBody;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.IncrementCounterTxBody;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ThrowingTxBody;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import javax.annotation.Nullable;

final class TransactionMessages {

  private static final KeyPair TEST_KEY_PAIR = ed25519().generateKeyPair();

  /**
   * Returns a CreateCounterTx transaction message with the given name and signed with the test key
   * pair.
   */
  static TransactionMessage createCreateCounterTx(String counterName,
      int qaServiceId) {
    return createCreateCounterTx(counterName, qaServiceId, TEST_KEY_PAIR);
  }

  /**
   * Returns a CreateCounterTx transaction message with the given name and signed with the given key
   * pair.
   */
  static TransactionMessage createCreateCounterTx(String counterName,
      int qaServiceId, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(qaServiceId)
        .transactionId(CREATE_COUNTER.id())
        .payload(CreateCounterTxBody.newBuilder()
            .setName(counterName)
            .build())
        .sign(keyPair);
  }

  /**
   * Returns an error transaction message with the given arguments and signed with the test key
   * pair.
   */
  static TransactionMessage createErrorTx(int errorCode,
      @Nullable String errorDescription, int qaServiceId) {
    return testMessage()
        .serviceId(qaServiceId)
        .transactionId(VALID_ERROR.id())
        .payload(ErrorTxBody.newBuilder()
            .setSeed(0)
            .setErrorCode(errorCode)
            .setErrorDescription(Strings.nullToEmpty(errorDescription))
            .build())
        .build();
  }

  /**
   * Returns an increment counter transaction message with the given arguments and signed
   * with the test key pair.
   */
  static TransactionMessage createIncrementCounterTx(long seed,
      HashCode counterId, int qaServiceId) {
    return createIncrementCounterTx(seed, counterId, qaServiceId, TEST_KEY_PAIR);
  }

  /**
   * Returns an increment counter transaction message with the given arguments and signed
   * with the given key pair.
   */
  static TransactionMessage createIncrementCounterTx(long seed, HashCode counterId,
      int qaServiceId, KeyPair keys) {
    return TransactionMessage.builder()
      .serviceId(qaServiceId)
      .transactionId(INCREMENT_COUNTER.id())
      .payload(IncrementCounterTxBody.newBuilder()
          .setSeed(seed)
          .setCounterId(ByteString.copyFrom(counterId.asBytes()))
          .build())
      .sign(keys);
  }

  /**
   * Returns a ThrowingTx transaction message with the given seed and signed with the test key pair.
   */
  static TransactionMessage createThrowingTx(long seed, int qaServiceId) {
    return testMessage()
        .serviceId(qaServiceId)
        .transactionId(VALID_THROWING.id())
        .payload(ThrowingTxBody.newBuilder()
            .setSeed(seed)
            .build())
        .build();
  }

  /**
   * Creates an 'unknown' to this service transaction.
   */
  static TransactionMessage createUnknownTx(int qaServiceId, KeyPair keys) {
    return TransactionMessage.builder()
        .serviceId(qaServiceId)
        .transactionId(UNKNOWN_TX_ID)
        .payload(new byte[0])
        .sign(keys);
  }

  private static TransactionMessage.Builder testMessage() {
    return TransactionMessage.builder()
        .serviceId(0)
        .transactionId(0)
        .payload(ByteString.EMPTY)
        .signedWith(TEST_KEY_PAIR);
  }

  private TransactionMessages() {}
}
