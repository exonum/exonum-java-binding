package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.crypto.PublicKey;

class TransferTxData {
  final long seed;
  final PublicKey senderId;
  final PublicKey recipientId;
  final long amount;

  TransferTxData(long seed, PublicKey senderId, PublicKey recipientId, long amount) {
    this.seed = seed;
    this.senderId = senderId;
    this.recipientId = recipientId;
    this.amount = amount;
  }
}
