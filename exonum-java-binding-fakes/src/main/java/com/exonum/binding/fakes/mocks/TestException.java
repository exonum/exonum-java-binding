package com.exonum.binding.fakes.mocks;

/**
 * Exception that's used for testing purposes only
 */
public class TestException extends RuntimeException {

  /**
   * Simple constructor to just detect an exception of a specific type
   */
  public TestException() {
    super();
  }

  /**
   * Constructor with message to detect specific context
   * @param context
   */
  public TestException(String context) {
    super(context);
  }
}
