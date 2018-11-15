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

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class CoreSchemaProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void getHeightBeforeGenesisBlockTest() {
    assertSchema((schema) -> assertThrows(RuntimeException.class, schema::getHeight));
  }

  @Test
  void getAllBlockHashesTest() {
    assertSchema((schema) -> assertThat(schema.getAllBlockHashes()).isEmpty());
  }

  @Test
  void getBlockTransactionsTest() {
    assertSchema((schema) -> {
      long height = 0L;
      assertThat(schema.getBlockTransactions(height)).isEmpty();
    });
  }

  @Test
  void getActiveConfigurationBeforeGenesisBlock() {
    assertSchema((schema) ->
        assertThrows(RuntimeException.class, schema::getActualConfiguration));
  }

  private static void assertSchema(Consumer<CoreSchemaProxy> assertion) {
    try (MemoryDb db = MemoryDb.newInstance(); Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);
      assertion.accept(CoreSchemaProxy.newInstance(view));
    } catch (CloseFailuresException e) {
      System.out.println("Error: " + e.getLocalizedMessage());
    }
  }
}
