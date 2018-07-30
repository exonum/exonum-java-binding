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

package com.exonum.binding.service.adapters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.messages.TransactionExecutionException;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Fork;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class UserTransactionAdapterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Transaction transaction;

  @Mock
  private ViewFactory viewFactory;

  @InjectMocks
  private UserTransactionAdapter transactionAdapter;

  @Test
  public void execute_closesCleanerAfterExecution() throws TransactionExecutionException {
    long forkHandle = 0x0B;
    transactionAdapter.execute(forkHandle);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createFork(eq(forkHandle), ac.capture());

    Cleaner cleaner = ac.getValue();
    assertTrue(cleaner.isClosed());
  }

  @Test
  public void execute_rethrowsExecutionException() throws TransactionExecutionException {
    long forkHandle = 0x0A;
    byte errorCode = 1;
    TransactionExecutionException txError = new TransactionExecutionException(errorCode);

    Fork fork = setupViewFactory(forkHandle);
    doThrow(txError).when(transaction).execute(eq(fork));

    expectedException.expect(equalTo(txError));
    transactionAdapter.execute(forkHandle);
  }

  @Test
  public void execute_rethrowsRuntimeExceptions() throws TransactionExecutionException {
    long forkHandle = 0x0A;
    RuntimeException unexpectedTxError = new NullPointerException("foo");

    Fork fork = setupViewFactory(forkHandle);
    doThrow(unexpectedTxError).when(transaction).execute(eq(fork));

    expectedException.expect(equalTo(unexpectedTxError));
    transactionAdapter.execute(forkHandle);
  }

  private Fork setupViewFactory(long forkHandle) {
    Fork fork = mock(Fork.class);
    when(viewFactory.createFork(eq(forkHandle), any(Cleaner.class)))
        .thenReturn(fork);
    return fork;
  }
}
