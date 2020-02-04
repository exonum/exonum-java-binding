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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class PrefixedIntegrationTest {

  @Nested
  class WithIndex {
    final String namespace = "test-namespace";
    final String entryName = "test-index";
    Cleaner cleaner;
    TemporaryDb db;

    @BeforeEach
    void initializeDatabase(TestInfo info) throws CloseFailuresException {
      // Create the database
      cleaner = new Cleaner(info.getDisplayName());
      db = TemporaryDb.newInstance();
      cleaner.add(db::close);

      // Initialize the test index
      try (Cleaner initCleaner = new Cleaner()) {
        Fork fork = db.createFork(initCleaner);
        String fullName = namespace + "." + entryName;
        // Initialize the index
        ProofEntryIndexProxy<String> e1 = fork
            .getProofEntry(IndexAddress.valueOf(fullName), string());
        String value = "V1";
        e1.set(value);
        // Merge into the DB
        db.merge(fork);
      }
    }

    @Test
    void fromForkAccessForkFirst() {
      // Access the index from a Fork
      Fork fork = db.createFork(cleaner);
      String fullName = namespace + "." + entryName;
      ProofEntryIndexProxy<String> e1 = fork
          .getProofEntry(IndexAddress.valueOf(fullName), string());

      // Create a Prefixed Access to that namespace
      Prefixed prefixed = Prefixed.fromAccess(namespace, fork);

      // Check can modify the Fork-based Prefixed Access
      assertTrue(prefixed.canModify());

      // Try to access the same index from the Prefixed
      ProofEntryIndexProxy<String> e2 = prefixed
          .getProofEntry(IndexAddress.valueOf(entryName), string());

      // Check it is the same instance (which is required for correct operation with Forks):
      assertThat(e2).isSameAs(e1);
      // fixme: There is an issue with such caches: #getIndexAddress _may_ return full address
      //   (if (a) the prefixed is created with a Fork; (b) the index is cached there),
      //   or the same address that the user passed (if the index is not cached, or the prefixed
      //   was created from handle
    }

    @Test
    void fromForkAccessPrefixedFirst() {
      // Create the base access — a Fork
      Fork fork = db.createFork(cleaner);

      // Create a Prefixed Access to that namespace
      Prefixed prefixed = Prefixed.fromAccess(namespace, fork);
      // Access the index from the Prefixed
      ProofEntryIndexProxy<String> e1 = prefixed
          .getProofEntry(IndexAddress.valueOf(entryName), string());

      // Try to access the same index from the Fork
      String fullName = namespace + "." + entryName;
      ProofEntryIndexProxy<String> e2 = fork
          .getProofEntry(IndexAddress.valueOf(fullName), string());

      // Check it is the same instance (which is required for correct operation with Forks):
      assertThat(e2).isSameAs(e1);
    }

    @Test
    void fromReadonlyAccessSnapshotBaseFirst() {
      // Create the base access — a Snapshot
      Snapshot base = db.createSnapshot(cleaner);

      // Access the index from the base
      String fullName = namespace + "." + entryName;
      ProofEntryIndexProxy<String> e1 = base
          .getProofEntry(IndexAddress.valueOf(fullName), string());

      // Create a Prefixed Access to that namespace
      Prefixed prefixed = Prefixed.fromAccess(namespace, base);

      // Check it inherits the immutability property
      assertFalse(prefixed.canModify());

      // Try to access the same index from the Prefixed
      ProofEntryIndexProxy<String> e2 = prefixed
          .getProofEntry(IndexAddress.valueOf(entryName), string());

      // Check the entry has the same _state_: readonly accesses do not require caching
      assertThat(e1.get()).isEqualTo(e2.get());
    }

    @Test
    void baseCleanerInvalidatesIndexesFromPrefixed() throws CloseFailuresException {
      // Create the base access — a Snapshot
      Snapshot base = db.createSnapshot(cleaner);

      // Create a Prefixed Access from the base
      Prefixed prefixed = Prefixed.fromAccess(namespace, base);
      // Create an index from the Prefixed
      ProofEntryIndexProxy<String> index = prefixed
          .getProofEntry(IndexAddress.valueOf(entryName), string());

      // Close the cleaner
      cleaner.close();

      // Check both the access and the index are inaccessible
      assertThrows(IllegalStateException.class, prefixed::getAccessNativeHandle);
      assertThrows(IllegalStateException.class, index::get);
    }

    @AfterEach
    void dropDatabase() throws CloseFailuresException {
      cleaner.close();
    }
  }
}
