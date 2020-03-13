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

package com.exonum.binding.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.service.ExecutionException;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.indices.TestProtoMessages;
import com.exonum.binding.core.transaction.Transaction;
import io.vertx.ext.web.Router;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class TransactionInvokerTest {

  static final int TRANSACTION_ID = 1;
  static final int TRANSACTION_ID_2 = 2;

  private static final byte[] ARGUMENTS = new byte[0];
  @Mock
  private ExecutionContext context;

  @Test
  void invokeValidServiceTransaction() {
    ValidService service = spy(new ValidService());
    TransactionInvoker invoker = new TransactionInvoker(service);
    invoker.invokeTransaction(TRANSACTION_ID, ARGUMENTS, context);
    invoker.invokeTransaction(TRANSACTION_ID_2, ARGUMENTS, context);

    verify(service).transactionMethod(ARGUMENTS, context);
    verify(service).transactionMethod2(ARGUMENTS, context);
  }

  @Test
  void invokeInvalidTransactionId() {
    TransactionInvoker invoker = new TransactionInvoker(new ValidService());
    int invalidTransactionId = Integer.MAX_VALUE;
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> invoker.invokeTransaction(invalidTransactionId, ARGUMENTS, context));
    assertThat(e.getMessage())
        .contains(String.format("No method with transaction id (%s)", invalidTransactionId));
  }

  @Test
  void invokeThrowingExecutionException() {
    ExecutionException e = new ExecutionException((byte) 0);
    TransactionInvoker invoker = new TransactionInvoker(new ThrowingAnyException(e));
    ExecutionException actual = assertThrows(ExecutionException.class,
        () -> invoker.invokeTransaction(TRANSACTION_ID, ARGUMENTS, context));
    assertThat(actual).isSameAs(e);
  }

  @Test
  void invokeThrowingRuntimeException() {
    RuntimeException e = new IllegalArgumentException("Unexpected runtime exception");
    TransactionInvoker invoker = new TransactionInvoker(new ThrowingAnyException(e));
    Exception actual = assertThrows(UnexpectedExecutionException.class,
        () -> invoker.invokeTransaction(TRANSACTION_ID, ARGUMENTS, context));
    assertThat(actual).hasCause(e);
  }

  @Test
  void invokeThrowingException() {
    IOException e = new IOException("Unexpected checked exception");
    TransactionInvoker invoker = new TransactionInvoker(new ThrowingAnyException(e));
    Exception actual = assertThrows(UnexpectedExecutionException.class,
        () -> invoker.invokeTransaction(TRANSACTION_ID, ARGUMENTS, context));
    assertThat(actual).hasCause(e);
  }

  @Test
  void invokeProtobufArgumentsService() {
    ProtobufArgumentsService service = spy(new ProtobufArgumentsService());
    TransactionInvoker invoker = new TransactionInvoker(service);
    TestProtoMessages.Point point = TestProtoMessages.Point.newBuilder()
        .setX(1)
        .setY(1)
        .build();

    invoker.invokeTransaction(TRANSACTION_ID, point.toByteArray(), context);

    verify(service).transactionMethod(point, context);
  }

  static class BasicService implements Service {

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      // no-op
    }
  }

  public static class ValidService extends BasicService {

    @Transaction(TRANSACTION_ID)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod(byte[] arguments, ExecutionContext context) {}

    @Transaction(TRANSACTION_ID_2)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod2(byte[] arguments, ExecutionContext context) {}
  }

  public static class ThrowingAnyException extends BasicService {
    private final Exception exception;

    public ThrowingAnyException(Exception e) {
      this.exception = e;
    }

    @Transaction(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments, ExecutionContext context) throws Exception {
      throw exception;
    }
  }

  public static class ProtobufArgumentsService extends BasicService {

    @Transaction(TRANSACTION_ID)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod(TestProtoMessages.Point arguments, ExecutionContext context) {}
  }
}
