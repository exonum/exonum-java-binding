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

package com.exonum.binding.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.TransactionSubmissionException;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.testkit.TestKit;
import org.junit.jupiter.api.Test;

class NodeProxyIntegrationTest {

  @Test
  void submitTransactionInvalidService() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);

      Node node = service.getNode();
      short unknownServiceId = (short) (TestService.SERVICE_ID + 1);
      RawTransaction unknownTx = RawTransaction.newBuilder()
          .payload(new byte[0])
          .serviceId(unknownServiceId)
          .transactionId((short) 1)
          .build();

      Exception e = assertThrows(TransactionSubmissionException.class,
          () -> node.submitTransaction(unknownTx));
      assertThat(e)
          .hasMessageContaining(Short.toString(unknownServiceId))
          .hasMessageContaining("not found");
    }
  }
}
