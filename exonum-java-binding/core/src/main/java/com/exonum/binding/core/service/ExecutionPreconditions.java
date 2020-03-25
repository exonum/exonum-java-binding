/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.service;

import static com.google.common.base.Strings.lenientFormat;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility methods that helps verifying conditions conducted in expression
 * while transaction execution.
 * If the condition is not met, the {@code ExecutionPreconditions} method
 * throws {@link ExecutionException}.
 *
 * <p>Consider the following example:
 * <pre>{@code
 *   void checkEnoughMoney(long balance, long amount) {
 *     if(balance < amount) {
 *       throw new ExecutionException((byte)3, "Not enough money. Operation amount is " + amount
 *       + ", but actual balance was " + balance);
 *     }
 *   }
 * }</pre>
 *
 * <p>which can be replaced using ExecutionPreconditions:
 * <pre>{@code
 *   checkExecution(amount <= balance, (byte)3,
 *       "Not enough money. Operation amount is %s, but actual balance was %s",
 *       amount, balance);
 * }</pre>
 *
 * @see ExecutionException
 */
public final class ExecutionPreconditions {

  /**
   * Verifies the truth of the given expression.
   *
   * @param expression a boolean expression
   * @param errorCode execution error code
   * @throws ExecutionException if {@code expression} is false
   */
  public static void checkExecution(boolean expression, byte errorCode) {
    if (!expression) {
      throw new ExecutionException(errorCode);
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * @param expression a boolean expression
   * @param errorCode execution error code
   * @param errorMessage execution error description to use if the check fails
   * @throws ExecutionException if {@code expression} is false
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable Object errorMessage) {
    if (!expression) {
      throw new ExecutionException(errorCode, String.valueOf(errorMessage));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * @param expression a boolean expression
   * @param errorCode execution error code
   * @param errorMessageTemplate execution error description template to use if the check fails.
   *        The template could have placeholders {@code %s} which will be replaced by arguments
   *        resolved by position
   * @param errorMessageArgs arguments to be used in the template. Each argument will be converted
   *        to string using {@link String#valueOf(Object)}
   * @throws ExecutionException if {@code expression} is false
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      @Nullable Object... errorMessageArgs) {
    if (!expression) {
      throw new ExecutionException(errorCode,
          lenientFormat(errorMessageTemplate, errorMessageArgs));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      int arg1) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      int arg1, int arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      int arg1, long arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      int arg1, @Nullable Object arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      long arg1) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      long arg1, int arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      long arg1, long arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      long arg1, @Nullable Object arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      @Nullable Object arg1) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      @Nullable Object arg1, int arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      @Nullable Object arg1, long arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      @Nullable Object arg1, @Nullable Object arg2) {
    if (!expression) {
      throw new ExecutionException(errorCode, lenientFormat(errorMessageTemplate, arg1, arg2));
    }
  }

  /**
   * Verifies the truth of the given expression.
   *
   * <p>See {@link #checkExecution(boolean, byte, String, Object...)} for details.
   */
  public static void checkExecution(boolean expression, byte errorCode,
      @Nullable String errorMessageTemplate,
      @Nullable Object arg1, @Nullable Object arg2, @Nullable Object arg3) {
    if (!expression) {
      throw new ExecutionException(errorCode,
          lenientFormat(errorMessageTemplate, arg1, arg2, arg3));
    }
  }

  private ExecutionPreconditions() {
  }
}
