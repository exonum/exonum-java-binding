package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.crypto.PublicKey;

class CreateWalletTxData {
  final PublicKey ownerPublicKey;
  final long initialBalance;

  CreateWalletTxData(PublicKey ownerPublicKey, long initialBalance) {
    this.ownerPublicKey = ownerPublicKey;
    this.initialBalance = initialBalance;
  }
}
