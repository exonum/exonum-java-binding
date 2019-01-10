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

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.hash.HashCode;
import org.junit.jupiter.api.Test;

class HistoryEntitySerializerTest {

  private HistoryEntitySerializer serializer = HistoryEntitySerializer.INSTANCE;

  @Test
  void roundTrip() {
    HistoryEntity expectedEntity = testEntity();

    byte[] bytes = serializer.toBytes(expectedEntity);
    HistoryEntity actualEntity = serializer.fromBytes(bytes);

    assertThat(actualEntity).isEqualTo(expectedEntity);
  }

  private static HistoryEntity testEntity() {

    return HistoryEntity.Builder.newBuilder()
        .setSeed(1L)
        .setWalletFrom(CryptoFunctions.ed25519().generateKeyPair().getPublicKey())
        .setWalletTo(CryptoFunctions.ed25519().generateKeyPair().getPublicKey())
        .setAmount(10L)
        .setTxMessageHash(HashCode.fromString("a0a0a0a0a0"))
        .build();
  }

}
