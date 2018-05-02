package com.exonum.binding.fakes.services.transactions;

import static com.exonum.binding.fakes.services.transactions.SetEntryTransaction.ENTRY_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.exonum.binding.util.LibraryLoader;
import org.junit.Test;

public class SetEntryTransactionIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void executePutsTheValueIntoEntry() throws CloseFailuresException {
    try (MemoryDb database = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      String value = "A value to set into entry";

      Fork fork = database.createFork(cleaner);
      SetEntryTransaction tx = new SetEntryTransaction(true, value, "");
      tx.execute(fork);

      database.merge(fork);

      Snapshot snapshot = database.createSnapshot(cleaner);
      try (EntryIndexProxy entry = EntryIndexProxy.newInstance(ENTRY_NAME, snapshot,
          StandardSerializers.string())) {
        assertTrue(entry.isPresent());
        assertThat(entry.get(), equalTo(value));
      }
    }
  }
}
