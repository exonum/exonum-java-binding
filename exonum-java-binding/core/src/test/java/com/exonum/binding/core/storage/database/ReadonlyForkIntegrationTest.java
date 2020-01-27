/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.database;

import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("ECR-xyz")
class ReadonlyForkIntegrationTest {

  private TemporaryDb db;
  private Cleaner cleaner;

  @BeforeEach
  void setupDb() {
    db = TemporaryDb.newInstance();
    cleaner = new Cleaner();
  }

  @AfterEach
  void tearDown() throws CloseFailuresException {
    try {
      if (cleaner != null) {
        cleaner.close();
      }
    } finally {
      db.close();
    }
  }

  @Test
  void roForkCanAccessIndex() throws CloseFailuresException {
    // Initialize an index in the database
    IndexAddress address = IndexAddress.valueOf("test");
    try (Cleaner forkCleaner = new Cleaner()) {
      Fork fork = db.createFork(forkCleaner);
      fork.getProofEntry(address, string())
          .set("V1");
      db.merge(fork);
    }

    Fork fork = db.createFork(cleaner);
    ReadonlyFork roFork = ReadonlyFork.fromFork(fork);

    // Check the created index is accessible
    ProofEntryIndexProxy<String> entry = roFork.getProofEntry(address, string());
    assertThat(entry.get()).isEqualTo("V1");

    // But, once accessed in RoFork, it becomes inaccessible from Fork (first wins):
    assertThrows(RuntimeException.class, () -> fork.getProofEntry(address, string()));
  }

  @Test
  void roForkForbidsModifications() {
    Fork fork = db.createFork(cleaner);
    ReadonlyFork roFork = ReadonlyFork.fromFork(fork);

    ProofEntryIndexProxy<String> entry = roFork
        .getProofEntry(IndexAddress.valueOf("test"), string());

    assertThrows(UnsupportedOperationException.class, () -> entry.set("V1"));
  }

  /*
  That's a limitation of single-scoped indexes: if an index is created with a Fork and no longer
  needed, it is currently not possible to destroy it and access it later with a ReadonlyFork.
  But, until that is needed, such limitation seems reasonable.
   */
  @Test
  void roForkCannotAccessSameIndexesAsFork() {
    IndexAddress address = IndexAddress.valueOf("test");
    Fork fork = db.createFork(cleaner);
    // Access the index
    fork.getProofEntry(address, string());

    ReadonlyFork roFork = ReadonlyFork.fromFork(fork);
    // Try to access it again — it is not possible/allowed
    assertThrows(RuntimeException.class,
        () -> roFork.getProofEntry(address, string()));
  }
}
