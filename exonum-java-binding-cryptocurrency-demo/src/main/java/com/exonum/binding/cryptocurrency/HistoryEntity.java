package com.exonum.binding.cryptocurrency;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class HistoryEntity {
  private final long seed;
  private final PublicKey walletFrom;
  private final PublicKey walletTo;
  private final long amount;
  private final HashCode transactionHash;

  private HistoryEntity(long seed, PublicKey walletFrom,
      PublicKey walletTo, long amount, HashCode transactionHash) {
    this.seed = seed;
    this.walletFrom = walletFrom;
    this.walletTo = walletTo;
    this.amount = amount;
    this.transactionHash = transactionHash;
  }

  public long getSeed() {
    return seed;
  }

  public PublicKey getWalletFrom() {
    return walletFrom;
  }

  public PublicKey getWalletTo() {
    return walletTo;
  }

  public long getAmount() {
    return amount;
  }

  public HashCode getTransactionHash() {
    return transactionHash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HistoryEntity that = (HistoryEntity) o;
    return seed == that.seed &&
        amount == that.amount &&
        Objects.equal(walletFrom, that.walletFrom) &&
        Objects.equal(walletTo, that.walletTo) &&
        Objects.equal(transactionHash, that.transactionHash);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seed, walletFrom, walletTo, amount, transactionHash);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("seed", seed)
        .add("walletFrom", walletFrom)
        .add("walletTo", walletTo)
        .add("amount", amount)
        .add("transactionHash", transactionHash)
        .toString();
  }

  public static final class Builder {
    private long seed;
    private PublicKey walletFrom;
    private PublicKey walletTo;
    private long amount;
    private HashCode transactionHash;

    private Builder() {
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public Builder setSeed(long seed) {
      this.seed = seed;
      return this;
    }

    public Builder setWalletFrom(PublicKey walletFrom) {
      this.walletFrom = walletFrom;
      return this;
    }

    public Builder setWalletTo(PublicKey walletTo) {
      this.walletTo = walletTo;
      return this;
    }

    public Builder setAmount(long amount) {
      this.amount = amount;
      return this;
    }

    public Builder setTransactionHash(HashCode transactionHash) {
      this.transactionHash = transactionHash;
      return this;
    }

    public HistoryEntity build() {
      return new HistoryEntity(seed, walletFrom, walletTo, amount, transactionHash);
    }
  }
}
