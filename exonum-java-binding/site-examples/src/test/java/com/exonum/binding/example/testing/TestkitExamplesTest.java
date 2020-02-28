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

package com.exonum.binding.example.testing;

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.example.testing.IntegrationTestArguments.ARTIFACTS_DIR;
import static com.exonum.binding.example.testing.IntegrationTestArguments.ARTIFACT_FILENAME;
import static com.exonum.binding.example.testing.IntegrationTestArguments.ARTIFACT_ID;
import static com.exonum.binding.fakeservice.FakeService.PUT_TX_ID;
import static com.exonum.binding.fakeservice.FakeService.RAISE_ERROR_TX_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.blockchain.ExecutionStatuses;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.fakeservice.FakeSchema;
import com.exonum.binding.fakeservice.Transactions.PutTransactionArgs;
import com.exonum.binding.fakeservice.Transactions.RaiseErrorArgs;
import com.exonum.binding.test.Integration;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.time.TimeSchema;
import com.exonum.messages.core.runtime.Errors.ExecutionError;
import com.exonum.messages.core.runtime.Errors.ExecutionStatus;
import com.google.protobuf.Empty;
import com.google.protobuf.MessageLite;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Integration
class TestkitExamplesTest {

  private static final String SERVICE_NAME = "test-service";
  private static final int SERVICE_ID = 17;
  private static final MessageLite SERVICE_CONFIGURATION = Empty.getDefaultInstance();
  private static final String SERVICE_NAME_2 = "test-service-2";
  private static final int SERVICE_ID_2 = 18;
  private static final KeyPair TEST_KEY_PAIR = ed25519().generateKeyPair();

  @SuppressWarnings("EmptyTryBlock")
  @Test
  @Disabled("This examples uses hard-coded constants for illustrative purposes — using "
      + "correct values will reduce the clarity")
  void testkitInstantiationForSingleService() {
    ServiceArtifactId artifactId =
        ServiceArtifactId.newJavaId("com.exonum.binding:test-service", "1.0.0");
    String artifactFilename = "test-service.jar";
    String serviceName = "test-service";
    int serviceId = 46;
    Path artifactsDirectory = Paths.get("target");
    try (TestKit testKit = TestKit.forService(artifactId, artifactFilename,
        serviceName, serviceId, artifactsDirectory)) {
      // Test logic
    }
  }

  @SuppressWarnings("EmptyTryBlock")
  @Test
  void testkitInstantiationUsingBuilder() {
    // This TestKit will be instantiated with two instances of the
    // same service ARTIFACT_ID.
    try (TestKit testKit = TestKit.builder()
        .withArtifactsDirectory(ARTIFACTS_DIR)
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        // The first service will be instantiated with some custom configuration
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        // The second — with no (empty) configuration
        .withService(ARTIFACT_ID, SERVICE_NAME_2, SERVICE_ID_2)
        .build()) {
      // Test logic
    }
  }

  @Test
  void validTransactionExecutionTest() {
    try (TestKit testKit = TestKit.forService(ARTIFACT_ID, ARTIFACT_FILENAME,
        SERVICE_NAME, SERVICE_ID, ARTIFACTS_DIR)) {
      // Construct a valid transaction
      TransactionMessage validTx = createPutTransaction(SERVICE_ID, "key1", "v1");

      // Commit block with this transaction
      testKit.createBlockWithTransactions(validTx);

      // Retrieve a snapshot of the current database state
      BlockchainData blockchainData = testKit.getBlockchainData(SERVICE_NAME);
      // It can be used to access the core schema, for example to check the
      // transaction execution result:
      Blockchain blockchain = blockchainData.getBlockchain();
      Optional<ExecutionStatus> txResult = blockchain.getTxResult(validTx.hash());
      assertThat(txResult).hasValue(ExecutionStatuses.SUCCESS);
      // And also to verify the changes the transaction made to the service state:
      FakeSchema schema = new FakeSchema(blockchainData.getExecutingServiceData());
      // Perform assertions on the data in the service schema
      MapIndex<String, String> testMap = schema.testMap();
      assertTrue(testMap.containsKey("key1"));
      assertThat(testMap.get("key1")).isEqualTo("v1");
    }
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
        .sign(TEST_KEY_PAIR);
  }

  @Test
  void errorTransactionExecutionTest() {
    try (TestKit testKit = TestKit.forService(ARTIFACT_ID, ARTIFACT_FILENAME,
        SERVICE_NAME, SERVICE_ID, ARTIFACTS_DIR)) {
      // Construct a transaction that will throw an ExecutionException
      // with the given error code during its execution
      byte errorCode = 100;
      TransactionMessage validTx = createErrorTransaction(SERVICE_ID, errorCode);

      // Commit block with this transaction
      testKit.createBlockWithTransactions(validTx);

      // Retrieve a snapshot of the current database state
      BlockchainData blockchainData = testKit.getBlockchainData(SERVICE_NAME);
      // Check that the transaction failed
      Blockchain blockchain = blockchainData.getBlockchain();
      Optional<ExecutionStatus> txResult = blockchain.getTxResult(validTx.hash());
      assertThat(txResult).isPresent();
      ExecutionStatus executionStatus = txResult.get();
      ExecutionError executionError = executionStatus.getError();
      assertThat(executionError.getCode()).isEqualTo(errorCode);
      assertThat(executionError.getDescription()).isEmpty();
      // ExecutionError also includes some properties of the error
      // that are set by the framework: error kind, call site, etc.
      // (not shown here, see ExecutionError API)
    }
  }

  private TransactionMessage createErrorTransaction(int serviceId, byte errorCode) {
    return TransactionMessage.builder()
        .serviceId(serviceId)
        .transactionId(RAISE_ERROR_TX_ID)
        .payload(
            RaiseErrorArgs.newBuilder()
                .setCode(errorCode)
                .build())
        .sign(TEST_KEY_PAIR);
  }

  @Test
  void createInPoolTransactions() {
    try (TestKit testKit = TestKit.forService(ARTIFACT_ID, ARTIFACT_FILENAME,
        SERVICE_NAME, SERVICE_ID, ARTIFACTS_DIR)) {
      // Create a block so that the `afterCommit` method is invoked
      testKit.createBlock();

      // This block will contain the in-pool transactions - namely, the transaction
      // submitted in the `afterCommit` method
      Block block = testKit.createBlock();

      // Check the resulting block or blockchain state
    }
  }

  // code-snippet ci_timeOracleTest {
  @Test
  void timeOracleTest() {
    String timeServiceName = "time-service";
    int timeServiceId = 10;
    ZonedDateTime initialTime = ZonedDateTime.now(ZoneOffset.UTC);
    FakeTimeProvider timeProvider = FakeTimeProvider.create(initialTime);
    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
        .withTimeService(timeServiceName, timeServiceId, timeProvider)
        .withArtifactsDirectory(ARTIFACTS_DIR)
        .build()) {
      // Create an empty block
      testKit.createBlock();
      // The time service submitted its first transaction in `afterCommit`
      // method, but it has not been executed yet
      Optional<ZonedDateTime> consolidatedTime1 =
          getConsolidatedTime(testKit, timeServiceName);
      // No time is available till the time service transaction is processed
      assertThat(consolidatedTime1).isEmpty();

      // Increase the time value
      ZonedDateTime time1 = initialTime.plusSeconds(1);
      timeProvider.setTime(time1);
      testKit.createBlock();
      // The time service submitted its second transaction. The first must
      // have been executed, with consolidated time now available and equal to
      // initialTime
      Optional<ZonedDateTime> consolidatedTime2 =
          getConsolidatedTime(testKit, timeServiceName);
      assertThat(consolidatedTime2).hasValue(initialTime);

      // Increase the time value
      ZonedDateTime time2 = initialTime.plusSeconds(1);
      timeProvider.setTime(time2);
      testKit.createBlock();
      // The time service submitted its third transaction, and processed the
      // second one. The consolidated time must be equal to time1
      Optional<ZonedDateTime> consolidatedTime3 =
          getConsolidatedTime(testKit, timeServiceName);
      assertThat(consolidatedTime3).hasValue(time1);
    }
  }

  private static Optional<ZonedDateTime> getConsolidatedTime(TestKit testKit,
      String timeServiceName) {
    BlockchainData blockchainData = testKit.getBlockchainData(timeServiceName);
    TimeSchema timeSchema = TimeSchema.newInstance(blockchainData, timeServiceName);
    return timeSchema.getTime().toOptional();
  }
  // }

  // code-snippet ci_registerExtension {
  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
          .withArtifactsDirectory(ARTIFACTS_DIR));

  @Test
  void test(TestKit testKit) {
    // Test logic
  }
  // }
}
