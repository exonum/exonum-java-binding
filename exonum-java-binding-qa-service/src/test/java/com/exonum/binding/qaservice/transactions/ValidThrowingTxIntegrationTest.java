package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.CreateCounterTxIntegrationTest.createCounter;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.util.LibraryLoader;
import org.junit.Test;

public class ValidThrowingTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void executeClearsQaServiceData() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Initialize storage with a counter equal to 10
      String name = "counter";
      long value = 10L;
      createCounter(view, name, value);

      // Create the transaction
      ValidThrowingTx tx = new ValidThrowingTx(0L);

      try {
        // Execute the transaction
        tx.execute(view);
        fail("#execute above must throw");
      } catch (IllegalStateException expected) {
        // Check that execute cleared the maps
        QaSchema schema = new QaSchema(view);
        try (MapIndex<HashCode, Long> counters = schema.counters();
             MapIndex<HashCode, String> counterNames = schema.counterNames()) {
          HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
          assertFalse(counters.containsKey(nameHash));
          assertFalse(counterNames.containsKey(nameHash));
        }

        // Check the exception message
        String message = expected.getMessage();
        assertThat(message, startsWith("#execute of this transaction always throws"));
      }
    }
  }
}
