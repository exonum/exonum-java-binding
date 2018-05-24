package com.exonum.binding.messages;

/**
 * Indicates that a transaction is not valid
 * (e.g., its {@link Transaction#isValid()} returns false).
 */
public final class InvalidTransactionException extends Exception {
  // todo: accept transaction and include it in error message?
  InvalidTransactionException(String message) {
    super(message);
  }
}
