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

package com.exonum.binding.test;

import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static com.exonum.binding.fakeservice.FakeService.PUT_TX_ID;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_DIR;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_FILENAME;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.blockchain.proofs.IndexProof;
import com.exonum.binding.core.runtime.DispatcherSchema;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.fakeservice.FakeSchema;
import com.exonum.binding.fakeservice.Transactions.PutTransactionArgs;
import com.exonum.binding.testkit.TestKit;
import com.exonum.messages.core.Proofs;
import com.exonum.messages.core.runtime.Lifecycle.InstanceState;
import com.exonum.messages.proof.MapProofOuterClass.MapProof;
import com.exonum.messages.proof.MapProofOuterClass.OptionalEntry;
import com.google.protobuf.ByteString;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test augments the IT from core, testing the aspects that are managed by the core:
 * accessing other service instances data.
 */
public class BlockchainDataIntegrationTest {

  private static final KeyPair KEY_PAIR = CryptoFunctions.ed25519().generateKeyPair();
  private static final String SERVICE_1_NAME = "test-service-1";
  private static final int SERVICE_1_ID = 101;
  private static final String SERVICE_2_NAME = "test-service-2";
  private static final int SERVICE_2_ID = 102;
  private TestKit testKit;

  @BeforeEach
  void setUp() {
    testKit = TestKit.builder()
        .withArtifactsDirectory(ARTIFACT_DIR)
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_1_NAME, SERVICE_1_ID)
        .withService(ARTIFACT_ID, SERVICE_2_NAME, SERVICE_2_ID)
        .build();
  }

  @AfterEach
  void destroyTestKit() {
    testKit.close();
  }

  @Test
  void getExecutingServiceData() {
    // Setup the executing service data
    String key = "k1";
    String value = "v1";
    TransactionMessage putTransaction = createPutTransaction(SERVICE_1_ID, key, value);
    testKit.createBlockWithTransactions(putTransaction);

    // Check that that data is accessible through BlockchainData by simple name
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    Prefixed serviceData = blockchainData.getExecutingServiceData();
    FakeSchema serviceSchema = new FakeSchema(serviceData);
    ProofMapIndexProxy<String, String> testMap = serviceSchema.testMap();

    assertThat(testMap.get(key)).isEqualTo(value);
  }

  @Test
  void findServiceDataOfOther() {
    // Setup the service 2 data
    String key = "k2";
    String value = "v2";
    TransactionMessage putTransaction = createPutTransaction(SERVICE_2_ID, key, value);
    testKit.createBlockWithTransactions(putTransaction);

    // Check that that data is accessible through BlockchainData _of service 1_ by simple name
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    Prefixed serviceData = blockchainData.findServiceData(SERVICE_2_NAME).get();
    IndexAddress address = IndexAddress.valueOf("test-map");
    ProofMapIndexProxy<String, String> testMap =
        serviceData.getProofMap(address, string(), string());
    assertThat(testMap.get(key)).isEqualTo(value);

    // Check it is read-only
    assertThrows(UnsupportedOperationException.class, () -> testMap.put("k", "v"));

    // Check it is accessible again
    Prefixed serviceDataAgain = blockchainData.findServiceData(SERVICE_2_NAME).get();
    assertDoesNotThrow(() -> serviceDataAgain.getProofMap(address, string(), string()));
  }

  @Test
  void findServiceDataOfThis() {
    // Setup the service 1 data
    String key = "k1";
    String value = "v1";
    TransactionMessage putTransaction = createPutTransaction(SERVICE_1_ID, key, value);
    testKit.createBlockWithTransactions(putTransaction);

    // Check that that data is accessible through BlockchainData of service 1 by simple name
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    Prefixed serviceData = blockchainData.findServiceData(SERVICE_1_NAME).get();
    FakeSchema serviceSchema = new FakeSchema(serviceData);
    ProofMapIndexProxy<String, String> testMap = serviceSchema.testMap();

    assertThat(testMap.get(key)).isEqualTo(value);
  }

  @Test
  void findServiceDataOfNonExistingService() {
    // Check that it is not possible to access the data of non-started service
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    Optional<Prefixed> serviceData = blockchainData.findServiceData("non-existing");
    assertThat(serviceData).isEmpty();
  }

  @Test
  void createIndexProof() {
    // Setup the service 1 data, to initialize the test proof map.
    TransactionMessage putTransaction = createPutTransaction(SERVICE_1_ID, "k1", "v1");
    testKit.createBlockWithTransactions(putTransaction);

    // Create the blockchainData for service 1
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    // Request the proof for the service merkelized collection by simple name
    String testMapName = "test-map";
    IndexProof indexProof = blockchainData.createIndexProof(testMapName);

    // Check the proof. Perform only basic verification — the proof creation is tested
    // in Blockchain IT.
    Prefixed serviceData = blockchainData.getExecutingServiceData();
    FakeSchema serviceSchema = new FakeSchema(serviceData);
    String expectedMapName = SERVICE_1_NAME + "." + testMapName;
    HashCode expectedIndexHash = serviceSchema.testMap().getIndexHash();
    OptionalEntry expectedEntry = OptionalEntry.newBuilder()
        .setKey(ByteString.copyFromUtf8(expectedMapName))
        .setValue(ByteString.copyFrom(expectedIndexHash.asBytes()))
        .build();

    Proofs.IndexProof proof = indexProof.getAsMessage();
    MapProof aggregatingIndexProof = proof.getIndexProof();
    assertThat(aggregatingIndexProof.getEntriesList()).containsExactly(expectedEntry);
  }

  @Test
  void createIndexProofUninitialized() {
    // Don't setup the service data, so that the test proof map remains uninitialized.
    // Create the blockchainData for service 1
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    // Request the proof for the service merkelized collection by simple name
    String testMapName = "test-map";
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> blockchainData.createIndexProof(testMapName));
    assertThat(e.getMessage()).containsIgnoringCase("does not exist");
  }

  @Test
  void getBlockchainSmokeTest() {
    Block block = testKit.createBlock();
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    Blockchain blockchain = blockchainData.getBlockchain();
    assertThat(blockchain.getLastBlock()).isEqualTo(block);
  }

  @Test
  void getDispatcherSchemaSmokeTest() {
    BlockchainData blockchainData = getBlockchainData(SERVICE_1_NAME);
    DispatcherSchema dispatcherSchema = blockchainData.getDispatcherSchema();
    ProofMapIndexProxy<String, InstanceState> instances = dispatcherSchema
        .serviceInstances();
    assertThat(instances.containsKey(SERVICE_1_NAME));
    assertThat(instances.containsKey(SERVICE_2_NAME));
  }

  private static TransactionMessage createPutTransaction(int serviceId, String key, String value) {
    return TransactionMessage.builder()
        .serviceId(serviceId)
        .transactionId(PUT_TX_ID)
        .payload(
            PutTransactionArgs.newBuilder()
                .setKey(key)
                .setValue(value)
                .build())
        .sign(KEY_PAIR);
  }

  private BlockchainData getBlockchainData(String serviceName) {
    return testKit.getBlockchainData(serviceName);
  }
}
