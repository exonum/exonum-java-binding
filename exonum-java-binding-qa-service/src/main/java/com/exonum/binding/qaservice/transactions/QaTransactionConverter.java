package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.QaTransaction.CREATE_COUNTER;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INVALID;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INVALID_THROWING;
import static com.exonum.binding.qaservice.transactions.QaTransaction.VALID_THROWING;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.service.TransactionConverter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

/** A converter of QA service transaction messages. */
public final class QaTransactionConverter implements TransactionConverter {

  @VisibleForTesting
  static final ImmutableMap<Short, Function<BinaryMessage, Transaction>> TRANSACTION_FACTORIES =
      ImmutableMap.of(
          INCREMENT_COUNTER.id(), IncrementCounterTx.converter()::fromMessage,
          CREATE_COUNTER.id(), CreateCounterTx.converter()::fromMessage,
          INVALID.id(), InvalidTx.converter()::fromMessage,
          INVALID_THROWING.id(), InvalidThrowingTx.converter()::fromMessage,
          VALID_THROWING.id(), ValidThrowingTx.converter()::fromMessage
      );

  @Override
  public Transaction toTransaction(BinaryMessage message) {
    checkServiceId(message);

    short txId = message.getMessageType();
    return TRANSACTION_FACTORIES.getOrDefault(txId, (m) -> {
      throw new IllegalArgumentException("Unknown transaction id: " + txId);
    })
        .apply(message);
  }

  private static void checkServiceId(BinaryMessage message) {
    short serviceId = message.getServiceId();
    checkArgument(serviceId == QaService.ID,
        "Wrong service id (%s), must be %s", serviceId, QaService.ID);
  }
}
