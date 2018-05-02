package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction.CREATE_WALLET;
import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction.TRANSFER;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.TransactionConverter;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

/** A converter of cryptocurrency service transaction messages. */
public final class CryptocurrencyTransactionConverter implements TransactionConverter {

  private static final ImmutableMap<Short, Function<BinaryMessage, Transaction>>
      TRANSACTION_FACTORIES =
          ImmutableMap.of(
              CREATE_WALLET.getId(), CreateWalletTx.converter()::fromMessage,
              TRANSFER.getId(), TransferTx.converter()::fromMessage);

  @Override
  public Transaction toTransaction(BinaryMessage message) {
    checkServiceId(message);

    short txId = message.getMessageType();
    return TRANSACTION_FACTORIES
        .getOrDefault(
            txId,
            (m) -> {
              throw new IllegalArgumentException("Unknown transaction id: " + txId);
            })
        .apply(message);
  }

  private static void checkServiceId(BinaryMessage message) {
    short serviceId = message.getServiceId();
    checkArgument(
        serviceId == CryptocurrencyService.ID,
        "Wrong service id (%s), must be %s",
        serviceId,
        CryptocurrencyService.ID);
  }
}
