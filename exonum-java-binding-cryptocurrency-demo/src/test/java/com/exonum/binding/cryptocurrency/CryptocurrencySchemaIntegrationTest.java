package com.exonum.binding.cryptocurrency;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class CryptocurrencySchemaIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void getStateHashes() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);
      CryptocurrencySchema schema = new CryptocurrencySchema(view);

      HashCode walletsMerkleRoot = schema.wallets().getRootHash();
      ImmutableList<HashCode> expectedHashes = ImmutableList.of(walletsMerkleRoot);

      assertThat(schema.getStateHashes(), equalTo(expectedHashes));
    }
  }
}
