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

import com.exonum.binding.messages.Transaction;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThrowingTransactionsTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createThrowingIllegalArgument() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    Transaction transaction = ThrowingTransactions.createThrowing(exceptionType);

    expectedException.expect(exceptionType);
    transaction.isValid();
  }

  @Test
  public void createThrowingIllegalArgumentInInfo() {
    // Transaction#info is a default method, check it separately
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    Transaction transaction = ThrowingTransactions.createThrowing(exceptionType);

    expectedException.expect(exceptionType);
    transaction.info();
  }

  @Test
  public void createThrowingOutOfMemoryError() {
    Class<OutOfMemoryError> exceptionType = OutOfMemoryError.class;
    Transaction transaction = ThrowingTransactions.createThrowing(exceptionType);

    expectedException.expect(exceptionType);
    transaction.isValid();
  }

  @Test
  public void createThrowingFailsIfUninstantiableThrowable() {
    expectedException.expect(IllegalArgumentException.class);
    ThrowingTransactions.createThrowing(UninstantiableException.class);
  }

  @Test
  @Ignore // Unfortunately, we do not perform such checks at the moment: ECR-1988
  public void createThrowingFailsIfInvalidThrowable() {
    expectedException.expect(IllegalArgumentException.class);
    ThrowingTransactions.createThrowing(IOException.class);
  }

  abstract static class UninstantiableException extends Exception {}
}
