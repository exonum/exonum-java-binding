package com.exonum.binding.messages;

/**
 * Indicates that an internal error occurred during transaction processing.
 */
public final class InternalServerError extends Exception {

  InternalServerError(String message) {
    super(message);
  }
}
