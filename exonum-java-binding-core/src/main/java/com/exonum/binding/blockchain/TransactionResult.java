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

package com.exonum.binding.blockchain;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.transaction.TransactionExecutionException;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import java.util.OptionalInt;
import javax.annotation.Nullable;

/**
 * Returns a result of the transaction execution. This result may be either a success, or an error,
 * if execution has failed.
 * Errors might be either service-defined or unexpected. Service-defined errors consist of an error
 * code and an optional description. Unexpected errors include only a description.
 *
 * @see TransactionExecutionException
 */
@AutoValue
public abstract class TransactionResult {

  /** The maximum allowed user-defined transaction error code. */
  public static final int MAX_USER_DEFINED_ERROR_CODE = 255;

  /** The status code of a successfully executed transaction. */
  public static final int SUCCESSFUL_RESULT_STATUS_CODE = 256;

  /**
   * The status code of transaction execution corresponding to an <em>unexpected</em> error
   * during transaction execution (some unexpected runtime exception in Java, panic in Rust).
   */
  public static final int UNEXPECTED_ERROR_STATUS_CODE = 257;

  private static final TransactionResult SUCCESSFUL_RESULT = valueOf(Type.SUCCESS, null, null);

  /**
   * Returns a transaction result corresponding to successful execution.
   */
  public static TransactionResult successful() {
    return TransactionResult.SUCCESSFUL_RESULT;
  }

  /**
   * Creates a transaction result corresponding to a user-defined
   * {@linkplain TransactionExecutionException exception} during transaction execution
   * (or the corresponding Error in Rust services).
   *
   * @param errorCode a user-defined error code; must be in range [0; 255]
   * @param errorDescription an optional error description; may be null, which is considered
   *     as no description
   */
  public static TransactionResult error(int errorCode, @Nullable String errorDescription) {
    checkArgument(0 <= errorCode && errorCode <= MAX_USER_DEFINED_ERROR_CODE,
        "Error code must be in range [0; 255], but was %s", errorCode);
    return valueOf(Type.ERROR, errorCode, errorDescription);
  }

  /**
   * Creates a transaction result corresponding to an <em>unexpected</em> error during transaction
   * execution (some unexpected runtime exception in Java, panic in Rust).
   *
   * @param errorDescription an optional error description; may be null
   */
  public static TransactionResult unexpectedError(@Nullable String errorDescription) {
    return valueOf(Type.UNEXPECTED_ERROR, null, errorDescription);
  }

  private static TransactionResult valueOf(
      Type type, @Nullable Integer errorCode, @Nullable String errorDescription) {
    return new AutoValue_TransactionResult(
        type,
        errorCode == null ? OptionalInt.empty() : OptionalInt.of(errorCode),
        Strings.nullToEmpty(errorDescription));
  }

  /**
   * Returns the type of this transaction execution result.
   * @return {@code Type.SUCCESS} if transaction was successful
   *         {@code Type.ERROR} if there was a service-defined error during transaction execution
   *         {@code Type.UNEXPECTED_ERROR} if there was an unexpected error during transaction
   *         execution
   */
  public abstract Type getType();

  /**
   * Returns an error code of a transaction if its execution resulted in a service-defined error.
   * @return a transaction error code in case of a service-defined error, or
   *         {@code OptionalInt.empty()} otherwise
   */
  public abstract OptionalInt getErrorCode();

  /**
   * Returns the description of a transaction if its execution resulted in an error;
   * or an empty string if there is no description. Never {@code null}.
   */
  public abstract String getErrorDescription();

  /**
   * Return whether transaction was successful or not.
   * @return true if transaction was successful, false otherwise
   */
  public boolean isSuccessful() {
    return getType() == Type.SUCCESS;
  }

  /** A type of transaction execution result. */
  public enum Type {
    /** Indicates successful transaction execution. */
    SUCCESS,

    /** Indicates a service-defined error during transaction execution. */
    ERROR,

    /** Indicates an unexpected error during transaction execution. */
    UNEXPECTED_ERROR
  }
}
