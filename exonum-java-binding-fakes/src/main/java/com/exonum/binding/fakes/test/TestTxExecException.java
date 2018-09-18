package com.exonum.binding.fakes.test;

import com.exonum.binding.transaction.TransactionExecutionException;

/**
 * The main purpose of this class is to cover the test case of processing an execution result of
 * transaction against the subclass of {@link #TransactionExecutionException}.
 */
public class TestTxExecException extends TransactionExecutionException {

  /**
   * The constructor that gets called from native code.
   *
   * @param errorCode the transaction error code
   */
  public TestTxExecException(byte errorCode) {
    super(errorCode);
  }
}
