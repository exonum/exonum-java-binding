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
