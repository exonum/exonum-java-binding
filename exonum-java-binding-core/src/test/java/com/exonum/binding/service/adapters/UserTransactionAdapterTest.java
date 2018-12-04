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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTransactionAdapterTest {

  @Mock
  private Transaction transaction;

  @Mock
  private ViewFactory viewFactory;

  @InjectMocks
  private UserTransactionAdapter transactionAdapter;

  @Test
  void execute_closesCleanerAfterExecution() throws TransactionExecutionException {
    long forkHandle = 0x0B;
    byte[] txHashBytes = ByteBuffer.allocate(1).array();
    byte[] authorPkbytes = ByteBuffer.allocate(1).array();
    transactionAdapter.execute(forkHandle, txHashBytes, authorPkbytes);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createFork(eq(forkHandle), ac.capture());

    Cleaner cleaner = ac.getValue();
    assertTrue(cleaner.isClosed());
  }

  @Test
  void execute_rethrowsExecutionException() throws TransactionExecutionException {
    long forkHandle = 0x0A;
    HashCode txHash = HashCode.fromInt(1);
    PublicKey authorPk = PublicKey.fromHexString("123abc");
    byte errorCode = 1;
    TransactionExecutionException txError = new TransactionExecutionException(errorCode);

    doThrow(txError).when(transaction).execute(any(TransactionContext.class));

    TransactionExecutionException thrown = assertThrows(TransactionExecutionException.class,
        () -> transactionAdapter.execute(forkHandle, txHash.asBytes(), authorPk.toBytes()));
    assertThat(thrown, equalTo(txError));
  }

  @Test
  void execute_rethrowsRuntimeExceptions() throws TransactionExecutionException {
    long forkHandle = 0x0A;
    HashCode txHash = HashCode.fromInt(1);
    PublicKey authorPk = PublicKey.fromHexString("123abc");
    RuntimeException unexpectedTxError = new NullPointerException("foo");

    doThrow(unexpectedTxError).when(transaction).execute(any(TransactionContext.class));

    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> transactionAdapter.execute(forkHandle, txHash.asBytes(), authorPk.toBytes()));
    assertThat(thrown, equalTo(unexpectedTxError));
  }

  private Fork setupViewFactory(long forkHandle) {
    Fork fork = mock(Fork.class);
    when(viewFactory.createFork(eq(forkHandle), any(Cleaner.class)))
        .thenReturn(fork);
    return fork;
  }
}
