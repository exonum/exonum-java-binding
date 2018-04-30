package com.exonum.binding.fakes.mocks;

import com.exonum.binding.messages.Transaction;
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

  abstract static class UninstantiableException extends Exception {}
}
