/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.runtime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.transaction.TransactionExecutionException;

/**
 * An "unexpected" transaction execution exception indicates that any exception
 * but {@link com.exonum.binding.core.transaction.TransactionExecutionException} occurred
 * in a transaction method. The original exception is stored as <em>cause</em>.
 *
 * @see com.exonum.core.messages.Runtime.ErrorKind#UNEXPECTED
 */
public class UnexpectedTransactionExecutionException extends RuntimeException {

  /**
   * Creates a new unexpected execution exception.
   * @param cause an exception that occurred in a transaction; must not be null or an instance of
   *     {@link TransactionExecutionException}
   */
  public UnexpectedTransactionExecutionException(Throwable cause) {
    super(checkValidCause(cause));
  }

  private static Throwable checkValidCause(Throwable cause) {
    checkNotNull(cause, "null cause is not allowed");
    checkArgument(!(cause instanceof TransactionExecutionException));
    return cause;
  }
}
