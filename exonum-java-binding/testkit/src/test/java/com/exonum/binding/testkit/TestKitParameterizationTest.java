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

package com.exonum.binding.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TestKitParameterizationTest {

  private static final short TEMPLATE_VALIDATOR_COUNT = 1;
  private static final EmulatedNodeType TEMPLATE_NODE_TYPE = EmulatedNodeType.VALIDATOR;
  private static final short NEW_VALIDATOR_COUNT = 8;

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withNodeType(TEMPLATE_NODE_TYPE)
          .withService(TestServiceModule.class)
          .withValidators(TEMPLATE_VALIDATOR_COUNT));

  @Test
  void testDefaultTestKit(TestKit testKit) {
    // Check that main TestKit node is a validator
    assertThat(testKit.getEmulatedNode().getNodeType()).isEqualTo(TEMPLATE_NODE_TYPE);
    testKit.withSnapshot(verifyNumValidators(TEMPLATE_VALIDATOR_COUNT));
  }

  @Test
  void testTestKitAuditor(@Auditor TestKit testKit) {
    // Check that main TestKit node is an auditor
    assertThat(testKit.getEmulatedNode().getNodeType()).isEqualTo(EmulatedNodeType.AUDITOR);
  }

  @Test
  void testTestKitValidatorCount(@ValidatorCount(NEW_VALIDATOR_COUNT) TestKit testKit) {
    testKit.withSnapshot(verifyNumValidators(NEW_VALIDATOR_COUNT));
  }

  private static Function<Snapshot, Void> verifyNumValidators(int expected) {
    return (view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getActualConfiguration().validatorKeys().size())
          .isEqualTo(expected);
      return null;
    };
  }
}
