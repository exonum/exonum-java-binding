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
import static org.mockito.Mockito.mock;
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

class TransactionInvokerTest {

  @Test
  void invokeValidServiceTransaction() throws Exception {
    ValidService service = spy(new ValidService());
    TransactionInvoker invoker = new TransactionInvoker(service);
    byte[] arguments = new byte[0];
    TransactionContext context = mock(TransactionContext.class);
    invoker.invokeTransaction(ValidService.TRANSACTION_ID, arguments, context);

    verify(service).transactionMethod(arguments, context);
  }

  @Test
  void invokeInvalidTransactionId() {
    TransactionInvoker invoker = new TransactionInvoker(new ValidService());
    byte[] arguments = new byte[0];
    TransactionContext context = mock(TransactionContext.class);
    int invalidTransactionId = ValidService.TRANSACTION_ID + 1;
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> invoker.invokeTransaction(invalidTransactionId, arguments, context));
    assertThat(e.getMessage())
        .contains(String.format("No method with transaction id (%s)", invalidTransactionId));
  }

  @Test
  void invokeThrowingTransaction() {
    TransactionInvoker invoker = new TransactionInvoker(new ThrowingService());
    byte[] arguments = new byte[0];
    TransactionContext context = mock(TransactionContext.class);
    TransactionExecutionException e = assertThrows(TransactionExecutionException.class,
        () -> invoker.invokeTransaction(ThrowingService.TRANSACTION_ID, arguments, context));
    assertThat(e.getErrorCode()).isEqualTo(ThrowingService.ERROR_CODE);
  }

  class BasicService implements Service {

    static final int TRANSACTION_ID = 1;

    @Override
    public List<HashCode> getStateHashes(Snapshot snapshot) {
      return Collections.emptyList();
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      // no-op
    }
  }

  class ValidService extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod(byte[] arguments, TransactionContext context) {}
  }

  class ThrowingService extends BasicService {

    static final byte ERROR_CODE = 18;

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments, TransactionContext context)
        throws TransactionExecutionException {
      throw new TransactionExecutionException(ERROR_CODE);
    }
  }
}
