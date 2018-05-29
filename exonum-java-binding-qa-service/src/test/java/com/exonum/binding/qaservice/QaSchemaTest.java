package com.exonum.binding.qaservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.util.LibraryLoader;
import java.util.List;
import org.junit.Test;

public class QaSchemaTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void getStateHashesEmptyDb() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot snapshot = db.createSnapshot(cleaner);

      QaSchema schema = new QaSchema(snapshot);

      List<HashCode> stateHashes = schema.getStateHashes();

      assertThat(stateHashes)
          .hasSize(1);

      HashCode countersRootHash = schema.counters().getRootHash();
      assertThat(stateHashes.get(0))
          .isEqualTo(countersRootHash);
    }
  }
}
