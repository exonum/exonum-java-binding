package com.exonum.binding.cryptocurrency;

public final class Wallet {

  private final long balance;

  public Wallet(long balance) {
    this.balance = balance;
  }

  public long getBalance() {
    return balance;
  }
}
