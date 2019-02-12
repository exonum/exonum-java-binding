/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  private final HashCode txMessageHash;

  private HistoryEntity(long seed, PublicKey walletFrom,
      PublicKey walletTo, long amount, HashCode txMessageHash) {
    this.seed = seed;
    this.walletFrom = walletFrom;
    this.walletTo = walletTo;
    this.amount = amount;
    this.txMessageHash = txMessageHash;
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

  public HashCode getTxMessageHash() {
    return txMessageHash;
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
    return seed == that.seed
        && amount == that.amount
        && Objects.equal(walletFrom, that.walletFrom)
        && Objects.equal(walletTo, that.walletTo)
        && Objects.equal(txMessageHash, that.txMessageHash);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seed, walletFrom, walletTo, amount, txMessageHash);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("seed", seed)
        .add("walletFrom", walletFrom)
        .add("walletTo", walletTo)
        .add("amount", amount)
        .add("txMessageHash", txMessageHash)
        .toString();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private long seed;
    private PublicKey walletFrom;
    private PublicKey walletTo;
    private long amount;
    private HashCode txMessageHash;

    private Builder() {
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

    public Builder setTxMessageHash(HashCode txMessageHash) {
      this.txMessageHash = txMessageHash;
      return this;
    }

    public HistoryEntity build() {
      return new HistoryEntity(seed, walletFrom, walletTo, amount, txMessageHash);
    }
  }
}
