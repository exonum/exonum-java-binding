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

package com.exonum.client.response;

import static com.exonum.client.response.TransactionStatus.COMMITTED;
import static com.exonum.client.response.TransactionStatus.IN_POOL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.message.TransactionMessage;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TransactionResponseTest {

  @Test
  void toStringInPoolTxTest() {
    TransactionResponse response = new TransactionResponse(IN_POOL,
        mock(TransactionMessage.class),
        mock(TransactionResult.class),
        null
    );
    assertThat(response.toString(), allOf(
        not(containsString("executionResult")),
        not(containsString("location"))
    ));
  }

  @Test
  void toStringCommitedTxTest() {
    TransactionResponse response = new TransactionResponse(COMMITTED,
        mock(TransactionMessage.class),
        mock(TransactionResult.class),
        mock(TransactionLocation.class)
    );
    assertThat(response.toString(), allOf(
        containsString("executionResult"),
        containsString("location")
    ));
  }

  @Test
  void equalsTest() {
    EqualsVerifier.forClass(TransactionResponse.class)
        .verify();
  }

}
