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

package com.exonum.binding.fakes.mocks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD) // MockitoExtension is not thread-safe: see mockito/1630
class ThrowingTransactionsTest {

  @Mock
  private TransactionContext context;

  @Test
  void createThrowingIllegalArgumentInInfo() {
    // Transaction#info is a default method, check it separately
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    Transaction transaction = ThrowingTransactions.createThrowing(exceptionType);

    assertThrows(exceptionType, transaction::info);
  }

  @Test
  void createThrowingOutOfMemoryError() {
    Class<OutOfMemoryError> exceptionType = OutOfMemoryError.class;
    Transaction transaction = ThrowingTransactions.createThrowing(exceptionType);

    assertThrows(exceptionType, () -> transaction.execute(context));
  }

  @Test
  void createThrowingFailsIfUninstantiableThrowable() {
    assertThrows(IllegalArgumentException.class,
        () -> ThrowingTransactions.createThrowing(UninstantiableException.class));
  }

  @Test
  @Disabled("Unfortunately, we do not perform such checks at the moment: ECR-1988")
  void createThrowingFailsIfInvalidThrowable() {
    assertThrows(IllegalArgumentException.class,
        () -> ThrowingTransactions.createThrowing(IOException.class));
  }

  @Test
  void createThrowingExecutionException() {
    byte errorCode = 1;
    String description = "Foo";
    Transaction tx = ThrowingTransactions.createThrowingExecutionException(false,
        errorCode, description);

    TransactionExecutionException actual = assertThrows(TransactionExecutionException.class,
        () -> tx.execute(context));
    assertThat(actual.getErrorCode(), equalTo(errorCode));
    assertThat(actual.getMessage(), equalTo(description));
  }

  @Test
  void createThrowingExecutionExceptionSubclass() {
    byte errorCode = 1;
    String description = "Foo";
    Transaction tx = ThrowingTransactions.createThrowingExecutionException(true,
        errorCode, description);

    TransactionExecutionException actual = assertThrows(TestTxExecException.class,
        () -> tx.execute(context));
    assertThat(actual.getErrorCode(), equalTo(errorCode));
    assertThat(actual.getMessage(), equalTo(description));
  }

  private abstract static class UninstantiableException extends Exception {}
}
