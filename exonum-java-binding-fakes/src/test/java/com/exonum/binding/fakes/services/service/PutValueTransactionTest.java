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

package com.exonum.binding.fakes.services.service;

import static com.exonum.binding.fakes.services.service.PutValueTransaction.BODY_CHARSET;
import static com.exonum.binding.fakes.services.service.TestSchemaFactories.createTestSchemaFactory;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.InternalTransactionContext;
import com.exonum.binding.transaction.RawTransaction;
import org.junit.jupiter.api.Test;

class PutValueTransactionTest {

  @Test
  void from_WrongTransactionId() {
    RawTransaction transaction = RawTransaction.newBuilder()
        .serviceId(TestService.ID)
        .transactionId((short) -1)
        .payload(encode("any value"))
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> PutValueTransaction.from(transaction, TestSchema::new));
  }

  @Test
  void from_WrongService() {
    RawTransaction transaction = RawTransaction.newBuilder()
        .serviceId((short) -1)
        .transactionId(PutValueTransaction.ID)
        .payload(encode("any value"))
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> PutValueTransaction.from(transaction, TestSchema::new));
  }

  @Test
  @SuppressWarnings("unchecked")
  // No type parameters for clarity
  void execute() {
    String value = "A value to put";
    HashCode hash = Hashing.defaultHashFunction()
        .hashString(value, BODY_CHARSET);

    RawTransaction transaction = RawTransaction.newBuilder()
        .serviceId(TestService.ID)
        .transactionId(PutValueTransaction.ID)
        .payload(encode(value))
        .build();

    Fork fork = mock(Fork.class);
    TestSchema schema = mock(TestSchema.class);
    ProofMapIndexProxy testMap = mock(ProofMapIndexProxy.class);
    when(schema.testMap()).thenReturn(testMap);

    PutValueTransaction tx = PutValueTransaction.from(transaction,
        createTestSchemaFactory(fork, schema));

    InternalTransactionContext context = new InternalTransactionContext(fork,
        null, null);
    tx.execute(context);

    verify(testMap).put(eq(hash), eq(value));
  }

  private static byte[] encode(String value) {
    return value.getBytes(BODY_CHARSET);
  }
}
