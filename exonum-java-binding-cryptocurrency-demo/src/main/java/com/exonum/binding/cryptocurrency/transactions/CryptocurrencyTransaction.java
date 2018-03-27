package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.messages.Transaction;
import com.google.common.primitives.Shorts;

public enum CryptocurrencyTransaction {

    CREATE_WALLET(0, CreateWalletTx.class),
    TRANSFER(1, TransferTx.class);

    private final short id;
    private final Class<? extends Transaction> transactionClass;

    CryptocurrencyTransaction(int id, Class<? extends Transaction> transactionClass) {
        this.id = Shorts.checkedCast(id);
        this.transactionClass = transactionClass;
    }

    public short getId() {
        return id;
    }

    public Class<? extends Transaction> transactionClass() {
        return transactionClass;
    }
}
