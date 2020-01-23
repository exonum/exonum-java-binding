/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.transaction;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import javax.annotation.Nullable;

/**
 * An error occurred during the execution of a Service method. The execution exception includes an
 * integer error code, that may be either service-specific or operation-specific; and an optional
 * description — an exception message. Different error codes allow the clients of the operation to
 * distinguish between different error conditions.
 *
 * <p>Exonum translates this exception into an {@link ExecutionError} type with error kind equal to
 * {@linkplain ErrorKind#SERVICE}. The execution error copies the error code and the description
 * from this exception. Exonum saves it into the database in {@linkplain
 * Blockchain#getCallErrors(long) the registry of call errors}. Note that only the value of the
 * error code affects the blockchain state.
 *
 * <p>The other attributes of a Java exception — a stack trace, a cause, suppressed exceptions — are
 * not saved in the database. They are used for logging only.
 *
 * <h3>Requesting Execution Errors</h3>
 *
 * <p>An execution error, including the error code and description, can be requested:
 *
 * <ul>
 *   <li>by any Exonum Service, using {@link Blockchain#getCallErrors(long)}
 *   <li>by an authorized Light Client, using the Exonum endpoints. For example, when the clients
 *       requests the transaction information, it will get the execution error, if it occurred. See
 *       <a
 *       href="https://exonum.com/doc/version/0.13-rc.2/advanced/node-management/#transaction">the
 *       API endpoint documentation</a> for more information.
 * </ul>
 *
 * @see Blockchain#getTxResult(HashCode)
 * @see Blockchain#getCallErrors(long)
 * @see ExecutionStatus
 */
public class ExecutionException extends RuntimeException {

  // TODO: Consider using enums and taking their ordinal as the error code: ECR-2006?
  private final byte errorCode;

  /**
   * Constructs a new transaction exception with no description.
   *
   * @param errorCode the transaction error code
   */
  public ExecutionException(byte errorCode) {
    this(errorCode, null);
  }

  /**
   * Constructs a new transaction exception with the specified description.
   *
   * @param errorCode the transaction error code
   * @param description the error description. The detail description is saved for later retrieval
   *     by the {@link #getMessage()} method.
   */
  public ExecutionException(byte errorCode, @Nullable String description) {
    this(errorCode, description, null);
  }

  /**
   * Constructs a new transaction exception with the specified description and cause.
   *
   * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
   * incorporated in this exception’s detail message.
   *
   * @param errorCode the transaction error code
   * @param description the error description. The detail description is saved for later retrieval
   *     by the {@link #getMessage()} method.
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public ExecutionException(
      byte errorCode, @Nullable String description, @Nullable Throwable cause) {
    super(description, cause);
    this.errorCode = errorCode;
  }

  /**
   * Returns the transaction error code.
   *
   * @see ExecutionError#getCode()
   */
  @SuppressWarnings("unused") // Native API
  public final byte getErrorCode() {
    return errorCode;
  }

  /**
   * Returns a string representation of this error. Includes the actual class name, the optional
   * description and the error code.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName()).append(": ");

    String description = getLocalizedMessage();
    if (description != null) {
      sb.append(description).append(", ");
    }
    sb.append("errorCode=").append(errorCode);
    return sb.toString();
  }
}
