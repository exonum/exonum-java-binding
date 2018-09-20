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

import org.junit.jupiter.api.Test;

class WalletSerializerTest {

  private WalletSerializer serializer = WalletSerializer.INSTANCE;

  @Test
  void roundTrip() {
    Wallet expectedWallet = testWallet();

    byte[] bytes = serializer.toBytes(expectedWallet);
    Wallet actualWallet = serializer.fromBytes(bytes);

    assertThat(actualWallet).isEqualTo(expectedWallet);
  }

  private static Wallet testWallet() {
    return new Wallet(100L);
  }

}
