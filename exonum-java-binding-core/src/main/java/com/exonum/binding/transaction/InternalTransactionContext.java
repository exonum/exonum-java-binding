package com.exonum.binding.transaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;

public class InternalTransactionContext implements TransactionContext {
    private Fork fork;
    private HashCode hash;
    private PublicKey authorPK;


    public InternalTransactionContext(Fork fork, HashCode hash, PublicKey authorPK) {
        this.fork = fork;
        this.hash = hash;
        this.authorPK = authorPK;
    }

    @Override
    public Fork getFork() {
        return fork;
    }

    @Override
    public HashCode getTransactionMessageHash() {
        return hash;
    }

    @Override
    public PublicKey getAuthorPK() {
        return authorPK;
    }
}
