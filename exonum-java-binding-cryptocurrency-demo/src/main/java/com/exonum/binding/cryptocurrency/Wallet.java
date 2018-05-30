package com.exonum.binding.cryptocurrency;

public final class Wallet {

  private final String name;
  private final long balance;

  public Wallet(String name, long balance) {
    this.name = name;
    this.balance = balance;
  }

  public String getName() {
    return name;
  }

  public long getBalance() {
    return balance;
  }
}
