package com.exonum.binding.cryptocurrency;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HistoryEntitySerializerTest {

  private HistoryEntitySerializer serializer = HistoryEntitySerializer.INSTANCE;

  @Test
  void roundTrip() {
    HistoryEntity expectedPendingTransaction = testPendingTransaction();

    byte[] bytes = serializer.toBytes(expectedPendingTransaction);
    HistoryEntity actualPendingTransaction = serializer.fromBytes(bytes);

    assertThat(actualPendingTransaction).isEqualTo(expectedPendingTransaction);
  }

  private static HistoryEntity testPendingTransaction() {
    return HistoryEntity.newBuilder()
        .setSeed(1)
        .setWalletFrom(PublicKey.fromHexString("abcd"))
        .setWalletTo(PublicKey.fromHexString("acbd"))
        .setAmount(100L)
        .setTxMessageHash(HashCode.fromString("a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1"))
        .build();
  }
}
