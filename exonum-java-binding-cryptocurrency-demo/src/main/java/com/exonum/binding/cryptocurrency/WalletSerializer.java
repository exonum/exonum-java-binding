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

import com.exonum.binding.common.serialization.Serializer;
import com.google.protobuf.InvalidProtocolBufferException;

public enum WalletSerializer implements Serializer<Wallet> {
  INSTANCE;

  @Override
  public byte[] toBytes(Wallet value) {
    WalletProtos.Wallet wallet = WalletProtos.Wallet.newBuilder()
        .setBalance(value.getBalance())
        .build();
    return wallet.toByteArray();
  }

  @Override
  public Wallet fromBytes(byte[] binaryWallet) {
    Wallet wallet;
    try {
      WalletProtos.Wallet copiedWalletProtos = WalletProtos.Wallet.parseFrom(binaryWallet);
      wallet = new Wallet(copiedWalletProtos.getBalance());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate WalletProtos.Wallet instance from provided binary data", e);
    }
    return wallet;
  }
}
