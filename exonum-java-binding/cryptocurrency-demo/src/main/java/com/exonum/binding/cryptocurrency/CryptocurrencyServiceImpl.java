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

package com.exonum.binding.cryptocurrency;

import static com.exonum.binding.cryptocurrency.TransactionError.INSUFFICIENT_FUNDS;
import static com.exonum.binding.cryptocurrency.TransactionError.NON_POSITIVE_TRANSFER_AMOUNT;
import static com.exonum.binding.cryptocurrency.TransactionError.SAME_SENDER_AND_RECEIVER;
import static com.exonum.binding.cryptocurrency.TransactionError.UNKNOWN_RECEIVER;
import static com.exonum.binding.cryptocurrency.TransactionError.UNKNOWN_SENDER;
import static com.exonum.binding.cryptocurrency.TransactionError.WALLET_ALREADY_EXISTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.indices.ListIndex;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.cryptocurrency.transactions.TxMessageProtos;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** A cryptocurrency demo service. */
public final class CryptocurrencyServiceImpl extends AbstractService
    implements CryptocurrencyService {

  public static final int CREATE_WALLET_TX_ID = 1;
  public static final int TRANSFER_TX_ID = 2;

  @Nullable private Node node;

  @Inject
  public CryptocurrencyServiceImpl(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  protected CryptocurrencySchema createDataSchema(Access access) {
    String name = getName();
    return new CryptocurrencySchema(access, name);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this);
    controller.mountApi(router);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public Optional<Wallet> getWallet(PublicKey ownerKey) {
    checkBlockchainInitialized();

    return node.withSnapshot(
        (access) -> {
          CryptocurrencySchema schema = createDataSchema(access);
          MapIndex<PublicKey, Wallet> wallets = schema.wallets();

          return Optional.ofNullable(wallets.get(ownerKey));
        });
  }

  @Override
  public List<HistoryEntity> getWalletHistory(PublicKey ownerKey) {
    checkBlockchainInitialized();

    return node.withSnapshot(
        access -> {
          CryptocurrencySchema schema = createDataSchema(access);
          ListIndex<HashCode> walletHistory = schema.transactionsHistory(ownerKey);
          Blockchain blockchain = Blockchain.newInstance(access);
          MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();

          return walletHistory.stream()
              .map(txMessages::get)
              .map(this::createTransferHistoryEntry)
              .collect(toList());
        });
  }

  @Override
  @Transaction(CREATE_WALLET_TX_ID)
  public void createWallet(TxMessageProtos.CreateWalletTx arguments, TransactionContext context) {
    PublicKey ownerPublicKey = context.getAuthorPk();

    CryptocurrencySchema schema =
        new CryptocurrencySchema(context.getFork(), context.getServiceName());
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();

    checkExecution(!wallets.containsKey(ownerPublicKey), WALLET_ALREADY_EXISTS.errorCode);

    long initialBalance = arguments.getInitialBalance();
    checkArgument(
        initialBalance >= 0, "The initial balance (%s) must not be negative.", initialBalance);
    Wallet wallet = new Wallet(initialBalance);

    wallets.put(ownerPublicKey, wallet);
  }

  @Override
  @Transaction(TRANSFER_TX_ID)
  public void transfer(TxMessageProtos.TransferTx arguments, TransactionContext context) {
    long sum = arguments.getSum();
    checkExecution(
        0 < sum, NON_POSITIVE_TRANSFER_AMOUNT.errorCode, "Non-positive transfer amount: " + sum);

    PublicKey fromWallet = context.getAuthorPk();
    PublicKey toWallet = toPublicKey(arguments.getToWallet());
    checkExecution(!fromWallet.equals(toWallet), SAME_SENDER_AND_RECEIVER.errorCode);

    CryptocurrencySchema schema =
        new CryptocurrencySchema(context.getFork(), context.getServiceName());
    ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
    checkExecution(wallets.containsKey(fromWallet), UNKNOWN_SENDER.errorCode);
    checkExecution(wallets.containsKey(toWallet), UNKNOWN_RECEIVER.errorCode);

    Wallet from = wallets.get(fromWallet);
    Wallet to = wallets.get(toWallet);
    checkExecution(sum <= from.getBalance(), INSUFFICIENT_FUNDS.errorCode);

    // Update the balances
    wallets.put(fromWallet, new Wallet(from.getBalance() - sum));
    wallets.put(toWallet, new Wallet(to.getBalance() + sum));

    // Update the transaction history of each wallet
    HashCode messageHash = context.getTransactionMessageHash();
    schema.transactionsHistory(fromWallet).add(messageHash);
    schema.transactionsHistory(toWallet).add(messageHash);
  }

  private static PublicKey toPublicKey(ByteString s) {
    return PublicKey.fromBytes(s.toByteArray());
  }

  // todo: consider extracting in a TransactionPreconditions or
  //   ExecutionException, with proper lazy formatting: ECR-2746.
  /** Checks a transaction execution precondition, throwing if it is false. */
  private static void checkExecution(boolean precondition, byte errorCode) {
    checkExecution(precondition, errorCode, null);
  }

  private static void checkExecution(
      boolean precondition, byte errorCode, @Nullable String message) {
    if (!precondition) {
      throw new ExecutionException(errorCode, message);
    }
  }

  private HistoryEntity createTransferHistoryEntry(TransactionMessage txMessage) {
    try {
      TxMessageProtos.TransferTx txBody =
          TxMessageProtos.TransferTx.parseFrom(txMessage.getPayload());

      return HistoryEntity.newBuilder()
          .setSeed(txBody.getSeed())
          .setWalletFrom(txMessage.getAuthor())
          .setWalletTo(PublicKey.fromBytes(txBody.getToWallet().toByteArray()))
          .setAmount(txBody.getSum())
          .setTxMessageHash(txMessage.hash())
          .build();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }

  private void checkBlockchainInitialized() {
    checkState(node != null, "Service has not been fully initialized yet");
  }
}
