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
 *
 */

package com.exonum.binding.blockchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class CoreSchemaProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void getHeightBeforeGenesisBlockTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      assertThrows(RuntimeException.class, schema::getHeight);
    }
  }

  @Test
  void getAllBlockHashesTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      assertThat(schema.getAllBlockHashes()).isEmpty();
    }
  }

  @Test
  void getBlockTransactionsTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      long height = 0L;
      assertThat(schema.getBlockTransactions(height)).isEmpty();
    }
  }

  @Test
  void getBlocksTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      assertTrue(schema.getBlocks().isEmpty());
    }
  }

  @Test
  void getLastBlockBeforeGenesisBlockTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      assertThrows(RuntimeException.class, schema::getLastBlock);
    }
  }

  @Test
  void getTxMessagesTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      assertTrue(schema.getTxMessages().isEmpty());
    }
  }

  @Test
  void getTxResultsTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      assertTrue(schema.getTxResults().isEmpty());
    }
  }

  @Test
  void getTxLocationsTest() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      CoreSchemaProxy schema = CoreSchemaProxy.newInstance(view);
      assertTrue(schema.getTxLocations().isEmpty());
    }
  }
}
