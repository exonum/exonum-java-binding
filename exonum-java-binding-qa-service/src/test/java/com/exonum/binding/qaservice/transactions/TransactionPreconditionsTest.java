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
 */

package com.exonum.binding.qaservice.transactions;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.transaction.RawTransaction;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class TransactionPreconditionsTest {

  @Test
  void checkTransactionValid() {
    short txId = 0x01;
    RawTransaction tx = RawTransaction.newBuilder()
        .serviceId(QaService.ID)
        .transactionId(txId)
        .payload(Bytes.bytes())
        .build();

    RawTransaction actual = TransactionPreconditions.checkTransaction(tx, txId);

    assertThat(actual, sameInstance(tx));
  }

  @Test
  void checkTransactionOfAnotherService() {
    short txId = 0x01;
    short serviceId = 10;
    RawTransaction tx = RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(txId)
        .payload(Bytes.bytes())
        .build();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionPreconditions.checkTransaction(tx, txId));
    assertThat(e.getMessage(), matchesPattern("This message \\(.+\\) does not belong "
        + "to this service: wrong service id \\(10\\), must be " + QaService.ID));
  }

  @Test
  void checkTransactionOfAnotherType() {
    short expectedTxId = 20;
    short txId = 1;
    short serviceId = QaService.ID;
    RawTransaction tx = RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(txId)
        .payload(Bytes.bytes())
        .build();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionPreconditions.checkTransaction(tx, expectedTxId));
    assertThat(e.getMessage(), matchesPattern("This message \\(.+\\) "
        + "has wrong transaction id \\(1\\), must be " + expectedTxId));
  }

  @Test
  void checkTransactionCorrectSize() {
    short txId = 0x01;
    int body = 10;
    RawTransaction tx = RawTransaction.newBuilder()
        .serviceId(QaService.ID)
        .transactionId(txId)
        .payload(ByteBuffer.allocate(body).array())
        .build();

    RawTransaction actual = TransactionPreconditions.checkPayloadSize(tx, body);

    assertThat(actual, sameInstance(tx));
  }

  @Test
  void checkTransactionWrongSize() {
    short txId = 0x01;
    int expectedBody = 11;
    int body = 10;
    RawTransaction tx = RawTransaction.newBuilder()
        .serviceId(QaService.ID)
        .transactionId(txId)
        .payload(ByteBuffer.allocate(body).array())
        .build();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionPreconditions.checkPayloadSize(tx, expectedBody));
    assertThat(e.getMessage(), matchesPattern("This transaction \\(.+\\) "
        + "has wrong size \\(\\d+\\), expected \\d+ bytes"));
  }
}
