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

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;

import com.exonum.binding.common.serialization.Serializer;

public enum WalletSerializer implements Serializer<Wallet> {
  INSTANCE;

  private static final Serializer<WalletProtos.Wallet> walletProtobufSerializer =
      protobuf(WalletProtos.Wallet.class);

  @Override
  public byte[] toBytes(Wallet value) {
    WalletProtos.Wallet wallet = WalletProtos.Wallet.newBuilder()
        .setBalance(value.getBalance())
        .build();
    return wallet.toByteArray();
  }

  @Override
  public Wallet fromBytes(byte[] binaryWallet) {
    WalletProtos.Wallet copiedWalletProtos = walletProtobufSerializer.fromBytes(binaryWallet);
    return new Wallet(copiedWalletProtos.getBalance());
  }
}
