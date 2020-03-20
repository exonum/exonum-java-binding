/*
 * Copyright 2019 The Exonum Team
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.blockchain.CallInBlocks;
import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.service.AbstractServiceModule;
import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.service.ExecutionException;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.runtime.ServiceArtifactBuilder;
import com.exonum.binding.testkit.TestKit;
import com.exonum.messages.core.Blockchain.CallInBlock;
import com.exonum.messages.core.runtime.Errors.ErrorKind;
import com.exonum.messages.core.runtime.Errors.ExecutionError;
import io.vertx.ext.web.Router;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.Extension;

class ServiceRuntimeIntegrationTest {

  private static final byte AFTER_TX_ERROR_CODE = 17;

  @Disabled("ECR-4093")
  @Test
  void afterTransactionsExecutionException(@TempDir Path artifactDir) throws IOException {
    String artifactFilename = "service.jar";
    // Create a service artifact
    String pluginId = "1:com.acme/s1:1.0.0";
    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom(pluginId);
    new ServiceArtifactBuilder()
        .setPluginId(pluginId)
        .setPluginVersion(artifactId.getVersion())
        .addClass(ThrowingInAfterTxService.class)
        .addExtensionClass(ThrowingInAfterTxModule.class)
        .writeTo(artifactDir.resolve(artifactFilename));

    // Create a testkit with this service instance
    int serviceId = 1;
    try (TestKit testKit = TestKit.builder()
        .withArtifactsDirectory(artifactDir)
        .withDeployedArtifact(artifactId, artifactFilename)
        .withService(artifactId, "s1", serviceId)
        .build()) {
      // Create a block, to trigger 'afterTransaction'
      testKit.createBlock();

      // Verify the result of afterTransaction
      Snapshot snapshot = testKit.getSnapshot();
      Blockchain blockchain = Blockchain.newInstance(snapshot);
      ProofMapIndexProxy<CallInBlock, ExecutionError> callErrors = blockchain.getCallErrors(1L);
      CallInBlock afterTxId = CallInBlocks.afterTransactions(serviceId);
      assertTrue(callErrors.containsKey(afterTxId));
      ExecutionError executionError = callErrors.get(afterTxId);
      assertThat(executionError.getKind()).isEqualTo(ErrorKind.SERVICE);
      assertThat(executionError.getCode()).isEqualTo(AFTER_TX_ERROR_CODE);
    }
  }

  public static class ThrowingInAfterTxService implements Service {

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      // No handlers
    }

    @Override
    public void afterTransactions(ExecutionContext context) {
      throw new ExecutionException(AFTER_TX_ERROR_CODE);
    }
  }

  @Extension
  public static class ThrowingInAfterTxModule extends AbstractServiceModule {

    @Override
    protected void configure() {
      bind(Service.class).to(ThrowingInAfterTxService.class);
    }
  }
}
