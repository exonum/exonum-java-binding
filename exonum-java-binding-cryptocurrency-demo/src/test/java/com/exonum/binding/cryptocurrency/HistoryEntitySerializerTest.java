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
        .setTransactionHash(HashCode.fromString("a0a0a0a0a0"))
        .build();
  }

}
