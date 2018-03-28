package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.messages.Message;
import java.nio.ByteBuffer;

final class CryptocurrencyTransactionTemplate {

    private static final short INVALID_MESSAGE_TYPE = 0;

    /**
     * A template of any transaction of cryptocurrency service. It has an ID of cryptocurrency service,
     * empty body and all-zero signature.
     */
    private static final Message CRYPTOCURRENCY_TRANSACTION_TEMPLATE = new Message.Builder()
            .setServiceId(CryptocurrencyService.ID)
            .setMessageType(INVALID_MESSAGE_TYPE)
            .setBody(allocateReadOnly(0))
            .setSignature(allocateReadOnly(Message.SIGNATURE_SIZE))
            .build();

    /**
     * Creates a new builder of cryptocurrency service transaction.
     *
     * @param transactionId a message type of transaction
     * @return a message builder that has an getId of cryptocurrency service, the given transaction ID, empty body
     *     and all-zero signature
     */
    static Message.Builder newCryptocurrencyTransactionBuilder(short transactionId) {
        return new Message.Builder()
                .mergeFrom(CRYPTOCURRENCY_TRANSACTION_TEMPLATE)
                .setMessageType(transactionId);
    }

    private static ByteBuffer allocateReadOnly(int size) {
        return ByteBuffer.allocate(size).asReadOnlyBuffer();
    }

    private CryptocurrencyTransactionTemplate() {}
}
