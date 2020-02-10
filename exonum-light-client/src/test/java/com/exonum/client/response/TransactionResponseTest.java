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

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.client.response.TransactionStatus.COMMITTED;
import static com.exonum.client.response.TransactionStatus.IN_POOL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.exonum.binding.common.blockchain.ExecutionStatuses;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.google.protobuf.ByteString;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TransactionResponseTest {

  @Test
  void toStringInPoolTxTest() {
    TransactionResponse response =
        new TransactionResponse(IN_POOL,
        withTxMessage(),
        null,
        null);

    assertThat(response.toString(), allOf(
        not(containsString("executionResult")),
        not(containsString("location"))
    ));
  }

  @Test
  void toStringCommittedTxTest() {
    TransactionResponse response =
        new TransactionResponse(COMMITTED,
            withTxMessage(),
            ExecutionStatuses.SUCCESS,
            withTxLocation());

    assertThat(response.toString(), allOf(
        containsString("executionResult"),
        containsString("location")
    ));
  }

  @Test
  void equalsTest() {
    EqualsVerifier.forClass(TransactionResponse.class)
        .withPrefabValues(
            ByteString.class, ByteString.copyFromUtf8("a"), ByteString.copyFromUtf8("b"))
        .withPrefabValues(
            ExecutionStatus.class,
            ExecutionStatuses.SUCCESS,
            ExecutionStatus.newBuilder()
                .setError(ExecutionError.getDefaultInstance())
                .build())
        .verify();
  }

  private static TransactionMessage withTxMessage() {
    return TransactionMessage.builder()
        .serviceId(1)
        .transactionId(1)
        .payload(new byte[]{})
        .sign(ed25519().generateKeyPair());
  }

  private static TransactionLocation withTxLocation() {
    return TransactionLocation.valueOf(1L, 0);
  }

}
