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

package com.exonum.binding.core.blockchain;

import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static com.exonum.binding.core.blockchain.Blocks.aBlock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.DispatcherSchema;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Database;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.core.messages.Runtime.InstanceState;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Disabled("ECR-4169")
class BlockchainDataIntegrationTest {

  Cleaner cleaner;
  TemporaryDb db;

  @BeforeEach
  void setUp() {
    cleaner = new Cleaner();
    db = TemporaryDb.newInstance();
    cleaner.add(db::close);
  }

  @AfterEach
  void tearDown() throws CloseFailuresException {
    cleaner.close();
  }

  @Test
  void getExecutingServiceDataFromFork() throws CloseFailuresException {
    // Setup the service data
    String serviceName = "test-service";
    String simpleIndexName = "test-entry";
    try (Cleaner cl = new Cleaner()) {
      Fork fork = db.createFork(cl);

      // Use the full index name
      String fullIndexName = serviceName + "." + simpleIndexName;
      fork.getProofEntry(IndexAddress.valueOf(fullIndexName), string())
          .set("V1");

      db.merge(fork);
    }

    Fork fork = db.createFork(cleaner);
    BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, serviceName);
    Prefixed serviceData1 = blockchainData.getExecutingServiceData();
    // Check the service data is accessible
    IndexAddress relativeAddress = IndexAddress.valueOf(simpleIndexName);
    ProofEntryIndexProxy<String> entry1 = serviceData1.getProofEntry(relativeAddress, string());
    assertThat(entry1.get()).isEqualTo("V1");

    // Check the service data is accessible in case of several requests to getExecutingServiceData
    Prefixed serviceData2 = blockchainData.getExecutingServiceData();
    ProofEntryIndexProxy<String> entry2 = serviceData2.getProofEntry(relativeAddress, string());
    assertThat(entry2.get()).isEqualTo("V1");
  }

  @ParameterizedTest
  @MethodSource("accessConstructors")
  void getBlockchain(BiFunction<Database, Cleaner, AbstractAccess> accessCtor) {
    AbstractAccess access = accessCtor.apply(db, cleaner);
    BlockchainData blockchainData = BlockchainData.fromRawAccess(access, "test-service");

    Blockchain blockchain = blockchainData.getBlockchain();
    // Check it works
    MapIndex<HashCode, Block> blocks = blockchain.getBlocks();
    assertTrue(blocks.isEmpty());
    // Check it is *readonly*
    Block block = aBlock().build();
    HashCode blockHash = block.getBlockHash();
    assertThrows(UnsupportedOperationException.class, () -> blocks.put(blockHash, block));
  }

  @ParameterizedTest
  @MethodSource("accessConstructors")
  void getDispatcherSchema(BiFunction<Database, Cleaner, AbstractAccess> accessCtor) {
    AbstractAccess access = accessCtor.apply(db, cleaner);
    BlockchainData blockchainData = BlockchainData.fromRawAccess(access, "test-service");

    DispatcherSchema dispatcherSchema = blockchainData.getDispatcherSchema();
    // Check it works
    ProofMapIndexProxy<String, InstanceState> instances = dispatcherSchema.serviceInstances();
    assertTrue(instances.isEmpty());

    // Check it is readonly
    assertThrows(UnsupportedOperationException.class,
        () -> instances.put("test-service-2", InstanceState.getDefaultInstance()));
  }

  private static Collection<BiFunction<Database, Cleaner, AbstractAccess>> accessConstructors() {
    return ImmutableList.of(
        Database::createFork,
        Database::createSnapshot
    );
  }
}
