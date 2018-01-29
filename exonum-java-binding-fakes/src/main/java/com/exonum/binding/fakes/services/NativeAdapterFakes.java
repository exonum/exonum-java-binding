package com.exonum.binding.fakes.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.adapters.UserTransactionAdapter;

/**
 * Provides methods to create fakes of Service and Transaction adapters.
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Used in native code
public final class NativeAdapterFakes {

  /**
   * Creates a UserTransactionAdapter of a no-op transaction, returning an empty string
   * as its info.
   *
   * @param valid whether a transaction has to be valid (i.e., return true
   *              in its {@link Transaction#isValid()} method)
   */
  public static UserTransactionAdapter createTransactionMock(boolean valid) {
    return createTransactionMock(valid, "");
  }

  /**
   * Creates a UserTransactionAdapter of a no-op transaction.
   *
   * @param valid whether a transaction has to be valid (i.e., return true
   *              in its {@link Transaction#isValid()} method)
   * @param info a value to be returned by {@link UserTransactionAdapter#info()}
   */
  public static UserTransactionAdapter createTransactionMock(boolean valid,
                                                             String info) {
    UserTransactionAdapter tx = mock(UserTransactionAdapter.class);
    when(tx.isValid()).thenReturn(valid);
    when(tx.info()).thenReturn(info);
    return tx;
  }

  /** Creates a UserServiceAdapter mock builder. */
  public static UserServiceAdapterMockBuilder createServiceFakeBuilder() {
    return new UserServiceAdapterMockBuilder();
  }

  private NativeAdapterFakes() {}
}
