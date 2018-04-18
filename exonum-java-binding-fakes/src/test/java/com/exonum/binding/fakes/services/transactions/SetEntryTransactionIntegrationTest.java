package com.exonum.binding.fakes.services.transactions;

import static com.exonum.binding.fakes.services.transactions.SetEntryTransaction.ENTRY_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.SnapshotProxy;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.exonum.binding.util.LibraryLoader;
import org.junit.Test;

public class SetEntryTransactionIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void executePutsTheValueIntoEntry() {
    try (MemoryDb database = new MemoryDb()) {
      String value = "A value to set into entry";
      try (ForkProxy fork = database.createFork()) {
        SetEntryTransaction tx = new SetEntryTransaction(true, value, "");
        tx.execute(fork);
        database.merge(fork);
      }

      try (SnapshotProxy snapshot = database.createSnapshot()) {
        EntryIndexProxy entry = new EntryIndexProxy<>(ENTRY_NAME, snapshot,
            StandardSerializers.string());
        assertTrue(entry.isPresent());
        assertThat(entry.get(), equalTo(value));
      }
    }
  }
}
