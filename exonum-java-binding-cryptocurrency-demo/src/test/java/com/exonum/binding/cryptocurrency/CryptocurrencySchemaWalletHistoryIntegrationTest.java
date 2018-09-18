package com.exonum.binding.cryptocurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.cryptocurrency.transactions.TransferTxData;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class CryptocurrencySchemaWalletHistoryIntegrationTest {

  static {
    LibraryLoader.load();
  }

  private KeyPair keyPair = CryptoFunctions.ed25519().generateKeyPair();
  private TransferTxData testTransfer =
      new TransferTxData(1L, keyPair.getPublicKey(), keyPair.getPublicKey(), 10L);

  @Test
  void walletHistoryNoRecords() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);
      CryptocurrencySchema schema = new CryptocurrencySchema(view);

      assertTrue(schema.walletHistory(keyPair.getPublicKey()).isEmpty());
    }
  }

  @Test
  void walletHistoryWithRecords() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = db.createFork(cleaner);
      CryptocurrencySchema schema = new CryptocurrencySchema(fork);

      schema.changeWalletBalance(keyPair.getPublicKey(), 10L, testTransfer);

      ProofListIndexProxy<TransferTxData> history = schema.walletHistory(keyPair.getPublicKey());
      assertFalse(history.isEmpty());

      assertThat(history.get(0)).isEqualTo(testTransfer);
    }
  }

}
