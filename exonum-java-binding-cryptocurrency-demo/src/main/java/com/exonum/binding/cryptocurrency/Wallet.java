package com.exonum.binding.cryptocurrency;

import com.exonum.binding.crypto.PublicKey;

public final class Wallet {

  private final PublicKey publicKey;
  private final long balance;

  public Wallet(PublicKey publicKey, long balance) {
    this.publicKey = publicKey;
    this.balance = balance;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  public long getBalance() {
    return balance;
  }
}
