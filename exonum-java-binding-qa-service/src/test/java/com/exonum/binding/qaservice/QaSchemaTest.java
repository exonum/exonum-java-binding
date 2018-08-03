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
import org.junit.jupiter.api.Test;

class QaSchemaTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void getStateHashesEmptyDb() throws CloseFailuresException {
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
