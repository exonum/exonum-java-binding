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

package com.exonum.binding.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.test.Bytes;
import org.junit.jupiter.api.Test;

class RawTransactionTest {

  @Test
  void builderTest() {
    short serviceId = 0x0A;
    short transactionId = 0x0B;
    byte[] payload = Bytes.bytes(0x00, 0x01, 0x02);

    RawTransaction transaction = RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(transactionId)
        .payload(payload)
        .build();

    assertThat(transaction.getTransactionId()).isEqualTo(transactionId);
    assertThat(transaction.getServiceId()).isEqualTo(serviceId);
    assertThat(transaction.getPayload()).isEqualTo(payload);
  }

}
