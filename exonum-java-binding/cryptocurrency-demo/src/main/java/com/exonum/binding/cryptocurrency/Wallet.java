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
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.protobuf.ByteString;

public final class Wallet {

  private final long balance;
  private final long pendingBalance;
  private final PublicKey signer;

  public Wallet(long balance, long pendingBalance, PublicKey signer) {
    this.balance = balance;
    this.pendingBalance = pendingBalance;
    this.signer = signer;
  }

  public long getPendingBalance() {
    return pendingBalance;
  }

  public PublicKey getSigner() {
    return signer;
  }

  public long getBalance() {
    return balance;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("balance", balance)
      .add("pendingBalance", pendingBalance)
      .add("signer", signer)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Wallet)) {
      return false;
    }
    Wallet wallet = (Wallet) o;
    return getBalance() == wallet.getBalance()
      && getPendingBalance() == wallet.getPendingBalance()
      && Objects.equal(getSigner(), wallet.getSigner());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getBalance(), getPendingBalance(), getSigner());
  }

  public static PublicKey toPublicKey(ByteString s) {
    return PublicKey.fromBytes(s.toByteArray());
  }

}
