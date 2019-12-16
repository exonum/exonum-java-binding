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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.core.transaction.TransactionMethod;
import io.vertx.ext.web.Router;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class TransactionInvokerTest {

  private static final byte[] ARGUMENTS = new byte[0];
  @Mock
  private TransactionContext context;

  @Test
  void invokeValidServiceTransaction() throws Exception {
    ValidService service = spy(new ValidService());
    TransactionInvoker invoker = new TransactionInvoker(service);
    invoker.invokeTransaction(ValidService.TRANSACTION_ID, ARGUMENTS, context);
    invoker.invokeTransaction(ValidService.TRANSACTION_ID_2, ARGUMENTS, context);

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
  void invokeThrowingTransactionExecutionException() {
    TransactionInvoker invoker = new TransactionInvoker(new ThrowingService());
    TransactionExecutionException e = assertThrows(TransactionExecutionException.class,
        () -> invoker.invokeTransaction(ThrowingService.TRANSACTION_ID, ARGUMENTS, context));
    assertThat(e.getErrorCode()).isEqualTo(ThrowingService.ERROR_CODE);
  }

  @Test
  void invokeThrowingServiceException() {
    TransactionInvoker invoker = new TransactionInvoker(new ThrowingService());
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> invoker.invokeTransaction(ThrowingService.TRANSACTION_ID_2, ARGUMENTS, context));
    assertThat(e.getCause().getClass()).isEqualTo(IllegalArgumentException.class);
  }

  static class BasicService implements Service {

    static final int TRANSACTION_ID = 1;
    static final int TRANSACTION_ID_2 = 2;

    @Override
    public List<HashCode> getStateHashes(Snapshot snapshot) {
      return Collections.emptyList();
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      // no-op
    }
  }

  public static class ValidService extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod(byte[] arguments, TransactionContext context) {
    }

    @TransactionMethod(TRANSACTION_ID_2)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod2(byte[] arguments, TransactionContext context) {
    }
  }

  public static class ThrowingService extends BasicService {

    static final byte ERROR_CODE = 18;
    static final String ERROR_MESSAGE = "Service originated exception";

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments, TransactionContext context)
        throws TransactionExecutionException {
      throw new TransactionExecutionException(ERROR_CODE);
    }

    @TransactionMethod(TRANSACTION_ID_2)
    public void transactionMethod2(byte[] arguments, TransactionContext context)
        throws TransactionExecutionException {
      throw new IllegalArgumentException(ERROR_MESSAGE);
    }
  }
}
