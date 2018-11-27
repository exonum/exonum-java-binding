package com.exonum.binding.transaction;

public interface TransactionConverter {
    public Transaction toTransaction(RawTransaction rawTx);
}
