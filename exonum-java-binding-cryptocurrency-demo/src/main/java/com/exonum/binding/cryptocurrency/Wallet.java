package com.exonum.binding.cryptocurrency;

import java.io.Serializable;

public final class Wallet implements Serializable {

  private static final long serialVersionUID = 1L;

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
