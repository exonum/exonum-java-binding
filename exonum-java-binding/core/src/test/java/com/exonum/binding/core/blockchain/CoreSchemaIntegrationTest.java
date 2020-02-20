/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.blockchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class CoreSchemaIntegrationTest {

  @Test
  void getHeightBeforeGenesisBlockTest() {
    assertSchema((schema) -> assertThrows(IllegalStateException.class, schema::getHeight));
  }

  @Test
  void getAllBlockHashesTest() {
    assertSchema((schema) -> assertThat(schema.getBlockHashes()).isEmpty());
  }

  @Test
  void getBlockTransactionsTest() {
    assertSchema((schema) -> {
      long height = 0L;
      Exception e = assertThrows(IllegalStateException.class,
          () -> schema.getBlockTransactions(height));
      assertThat(e).hasMessageContaining("No genesis block created");
    });
  }

  @Test
  void getConsensusConfigurationBeforeGenesisBlock() {
    assertSchema((schema) ->
        assertThrows(IllegalStateException.class, schema::getConsensusConfiguration));
  }

  @Test
  void getBlocksTest() {
    assertSchema((schema) -> assertTrue(schema.getBlocks().isEmpty()));
  }

  @Test
  void getTxMessagesTest() {
    assertSchema((schema) -> assertTrue(schema.getTxMessages().isEmpty()));
  }

  @Test
  void getTxLocationsTest() {
    assertSchema((schema) -> assertTrue(schema.getTxLocations().isEmpty()));
  }

  @Test
  void getTransactionPool() {
    assertSchema((schema) -> {
      Set<HashCode> set = ImmutableSet.copyOf(schema.getTransactionPool());
      assertTrue(set.isEmpty());
    });
  }

  private static void assertSchema(Consumer<CoreSchema> assertion) {
    try (TemporaryDb db = TemporaryDb.newInstance(); Cleaner cleaner = new Cleaner()) {
      Snapshot snapshot = db.createSnapshot(cleaner);
      assertion.accept(CoreSchema.newInstance(snapshot));
    } catch (CloseFailuresException e) {
      fail(e.getLocalizedMessage());
    }
  }
}
