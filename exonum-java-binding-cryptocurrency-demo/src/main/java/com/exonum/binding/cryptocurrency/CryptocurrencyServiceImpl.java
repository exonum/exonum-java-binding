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

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.cryptocurrency.transactions.TxMessageProtos;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.MapIndex;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

/** A cryptocurrency demo service. */
public final class CryptocurrencyServiceImpl extends AbstractService
    implements CryptocurrencyService {

  @Nullable private Node node;

  @Inject
  public CryptocurrencyServiceImpl(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return new CryptocurrencySchema(view);
  }

  @Override
  public Optional<String> initialize(Fork fork) {
    return Optional.empty();
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

    return node.withSnapshot((view) -> {
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();

      return Optional.ofNullable(wallets.get(ownerKey));
    });
  }

  @Override
  public List<HistoryEntity> getWalletHistory(PublicKey ownerKey) {
    checkBlockchainInitialized();

    return node.withSnapshot(view -> {
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ListIndex<HashCode> walletHistory = schema.transactionsHistory(ownerKey);
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();

      return StreamSupport.stream(walletHistory.spliterator(), false)
          .map(txMessages::get)
          .map(this::createTransferHistoryEntry)
          .collect(toList());
    });
  }

  private HistoryEntity createTransferHistoryEntry(TransactionMessage txMessage) {
    checkState(txMessage.getServiceId() == getId(),
        "service id mismatch: message contains %s, expected %s", txMessage.getServiceId(), getId());
    try {
      TxMessageProtos.TransferTx txBody = TxMessageProtos.TransferTx
          .parseFrom(txMessage.getPayload());

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
