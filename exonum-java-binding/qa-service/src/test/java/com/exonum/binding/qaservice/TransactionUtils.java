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

import static com.exonum.binding.common.hash.Hashing.defaultHashFunction;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.ThrowingTx;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.TestKit;

/**
 * Helper class to create transaction messages from raw transactions.
 */
public final class TransactionUtils {

  private static final HashCode DEFAULT_HASH = HashCode.fromString("a0b0c0d0");
  private static final PublicKey DEFAULT_AUTHOR_KEY = PublicKey.fromHexString("abcd");
  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  /**
   * Returns new context with default values for a given view.
   */
  public static TransactionContext newContext(Fork view) {
    return TransactionContext.builder()
        .fork(view)
        .txMessageHash(DEFAULT_HASH)
        .authorPk(DEFAULT_AUTHOR_KEY)
        .build();
  }

  /** Creates a counter in the storage with the given name and initial value. */
  public static void createCounter(Fork view, String name, Long initialValue) {
    HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);
    QaSchema schema = new QaSchema(view);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> counterNames = schema.counterNames();
    counters.put(nameHash, initialValue);
    counterNames.put(nameHash, name);
  }

  /**
   * Returns a CreateCounterTx transaction message with given name and signed with service key pair
   * of a given TestKit.
   */
  public static TransactionMessage createCreateCounterTransaction(
      String counterName, TestKit testKit) {
    RawTransaction rawTransaction = toRawCreateCounterTx(counterName);
    KeyPair emulatedNodeKeyPair = getTestKitNodeServiceKeyPair(testKit);
    return toTransactionMessage(rawTransaction, emulatedNodeKeyPair);
  }

  /**
   * Returns a CreateCounterTx transaction message with given name and signed with generated key
   * pair.
   */
  public static TransactionMessage createCreateCounterTransaction(String counterName) {
    RawTransaction rawTransaction = toRawCreateCounterTx(counterName);
    return toTransactionMessage(rawTransaction);
  }

  private static RawTransaction toRawCreateCounterTx(String counterName) {
    CreateCounterTx createCounterTx = new CreateCounterTx(counterName);
    return createCounterTx.toRawTransaction();
  }

  /**
   * Returns an IncrementCounterTx transaction message with given seed and counterId and signed
   * with service key pair of a given TestKit.
   */
  public static TransactionMessage createIncrementCounterTransaction(
      long seed, HashCode counterId, TestKit testKit) {
    RawTransaction rawTransaction = toRawIncrementCounterTx(seed, counterId);
    KeyPair emulatedNodeKeyPair = getTestKitNodeServiceKeyPair(testKit);
    return toTransactionMessage(rawTransaction, emulatedNodeKeyPair);
  }

  /**
   * Returns an IncrementCounterTx transaction message with given seed and counterId and signed
   * with generated key pair.
   */
  public static TransactionMessage createIncrementCounterTransaction(
      long seed, HashCode counterId) {
    RawTransaction rawTransaction = toRawIncrementCounterTx(seed, counterId);
    return toTransactionMessage(rawTransaction);
  }

  private static RawTransaction toRawIncrementCounterTx(long seed, HashCode counterId) {
    IncrementCounterTx incrementCounterTx = new IncrementCounterTx(seed, counterId);
    return incrementCounterTx.toRawTransaction();
  }

  /**
   * Returns a ThrowingTx transaction message with given seed and signed with service key pair of a
   * given TestKit.
   */
  public static TransactionMessage createThrowingTransaction(long seed, TestKit testKit) {
    RawTransaction rawTransaction = toRawThrowingTx(seed);
    KeyPair emulatedNodeKeyPair = getTestKitNodeServiceKeyPair(testKit);
    return toTransactionMessage(rawTransaction, emulatedNodeKeyPair);
  }

  /**
   * Returns a ThrowingTx transaction message with given seed and signed with generated key
   * pair.
   */
  public static TransactionMessage createThrowingTransaction(long seed) {
    RawTransaction rawTransaction = toRawThrowingTx(seed);
    return toTransactionMessage(rawTransaction);
  }

  private static KeyPair getTestKitNodeServiceKeyPair(TestKit testKit) {
    EmulatedNode emulatedNode = testKit.getEmulatedNode();
    return emulatedNode.getServiceKeyPair();
  }

  private static RawTransaction toRawThrowingTx(long seed) {
    ThrowingTx throwingTx = new ThrowingTx(seed);
    return throwingTx.toRawTransaction();
  }

  /**
   * Given a {@code rawTransaction}, signs it using ED25519 crypto function generated key pair and
   * returns a resulting transaction message.
   */
  public static TransactionMessage toTransactionMessage(RawTransaction rawTransaction) {
    KeyPair generatedKeyPair = CRYPTO_FUNCTION.generateKeyPair();
    return toTransactionMessage(rawTransaction, generatedKeyPair);
  }

  /**
   * Given a {@code rawTransaction}, signs it using given key pair and returns a resulting
   * transaction message.
   */
  public static TransactionMessage toTransactionMessage(
      RawTransaction rawTransaction, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(rawTransaction.getServiceId())
        .transactionId(rawTransaction.getTransactionId())
        .payload(rawTransaction.getPayload())
        .sign(keyPair, CRYPTO_FUNCTION);
  }
}
