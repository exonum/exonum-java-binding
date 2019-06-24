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

package com.exonum.binding.core.service.adapters;

import static com.exonum.binding.test.Bytes.randomBytes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTransactionAdapterTest {
  private static final long FORK_HANDLE = 0x0B;
  private static final byte[] TX_HASH = randomBytes(10);
  private static final byte[] AUTHOR_PK = randomBytes(10);

  @Mock
  private Transaction transaction;
  @Mock
  private ViewFactory viewFactory;
  @Mock
  private Fork fork;

  @InjectMocks
  private UserTransactionAdapter transactionAdapter;

  @Test
  void execute_closesCleanerAfterExecution() throws TransactionExecutionException {
    when(viewFactory.createFork(eq(FORK_HANDLE), any(Cleaner.class))).thenReturn(fork);

    transactionAdapter.execute(FORK_HANDLE, TX_HASH, AUTHOR_PK);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createFork(eq(FORK_HANDLE), ac.capture());

    Cleaner cleaner = ac.getValue();
    assertTrue(cleaner.isClosed());
  }

  @Test
  void execute_rethrowsExecutionException() throws TransactionExecutionException {
    when(viewFactory.createFork(eq(FORK_HANDLE), any(Cleaner.class))).thenReturn(fork);

    TransactionExecutionException txError = new TransactionExecutionException((byte) 0);

    doThrow(txError).when(transaction).execute(any(TransactionContext.class));

    TransactionExecutionException thrown = assertThrows(TransactionExecutionException.class,
        () -> transactionAdapter.execute(FORK_HANDLE, TX_HASH, AUTHOR_PK));
    assertThat(thrown, is(txError));
  }

  @Test
  void execute_rethrowsRuntimeExceptions() throws TransactionExecutionException {
    when(viewFactory.createFork(eq(FORK_HANDLE), any(Cleaner.class))).thenReturn(fork);

    RuntimeException unexpectedTxError = new RuntimeException("Unexpected execution exception");

    doThrow(unexpectedTxError).when(transaction).execute(any(TransactionContext.class));

    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> transactionAdapter.execute(FORK_HANDLE, TX_HASH, AUTHOR_PK));
    assertThat(thrown, is(unexpectedTxError));
  }

}
