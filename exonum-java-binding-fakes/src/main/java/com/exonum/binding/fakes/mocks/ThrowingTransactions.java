package com.exonum.binding.fakes.mocks;

import static org.mockito.Mockito.mock;

import com.exonum.binding.messages.Transaction;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A factory of throwing transaction mocks.
 */
public final class ThrowingTransactions {

  /**
   * Creates a mock of Transaction that throws an exception of the given type
   * in all of its methods.
   *
   * @param exceptionType a type of exception to throw
   * @throws IllegalArgumentException if exception type is un-instantiable (e.g, abstract)
   */
  public static Transaction createThrowing(Class<? extends Throwable> exceptionType) {
    Answer throwByDefault = new AlwaysThrowingAnswer(exceptionType);
    return mock(Transaction.class, throwByDefault);
  }

  private static class AlwaysThrowingAnswer implements Answer {

    private final Class<? extends Throwable> exceptionType;

    AlwaysThrowingAnswer(Class<? extends Throwable> exceptionType) {
      checkCanInstantiate(exceptionType);
      this.exceptionType = exceptionType;
    }

    private void checkCanInstantiate(Class<? extends Throwable> exceptionType) {
      try {
        // Try to create an exception of the given type, discard the instance if successful.
        exceptionType.newInstance();
      } catch (IllegalAccessException | InstantiationException e) {
        throw new IllegalArgumentException("Un-instantiable exception type: " + exceptionType, e);
      }
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
      throw exceptionType.newInstance();
    }
  }

  private ThrowingTransactions() {}
}
