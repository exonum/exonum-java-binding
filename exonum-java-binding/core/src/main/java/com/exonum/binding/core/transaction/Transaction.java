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

import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.service.Service;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method is a transaction method. The annotated method should execute the
 * transaction, possibly modifying the blockchain state.
 *
 * <p>The method should be a {@code public} {@link Service} method.
 *
 * <h3>Parameters</h3>
 *
 * <p>The annotated method shall have the following parameters (in this particular order):
 * <ol>
 *   <li>transaction arguments either as {@code byte[]} or as a protobuf message.
 *       Protobuf messages are deserialized using a {@code #parseFrom(byte[])} method
 *   <li>transaction execution context as {@link TransactionContext}. It allows to access
 *       the information about this transaction and modify the blockchain state
 *       through the included database fork.
 * </ol>
 *
 * <h3>Exceptions</h3>
 *
 * <p>The annotated method might throw {@linkplain TransactionExecutionException} if the
 * transaction cannot be executed normally and has to be rolled back. The transaction will be
 * committed as failed (error kind {@linkplain ErrorKind#SERVICE SERVICE}), the
 * {@linkplain ExecutionError#getCode() error code} with the optional description will be saved
 * into the storage. The client can request the error code to know the reason of the failure.
 *
 * <p>The annotated method might also throw any other exception if an unexpected error
 * <!-- todo: @slowli: Do you agree with the part 'if the clients do not need to distinguish between
 *   different error types' or prefer the previous recommendation? -->
 * occurs, or if the clients do not need to distinguish between different error types.
 * The transaction will be committed as failed (error kind
 * {@linkplain ErrorKind#UNEXPECTED UNEXPECTED}).
 *
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/architecture/transactions">Exonum Transactions</a>
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/architecture/services">Exonum Services</a>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transaction {

  /**
   * The transaction type identifier. Must be unique within the service.
   * <!-- TODO: update to 'Exonum interface' or sth when they are supported: ECR-3783 -->
   *
   * <p>The transaction id is specified in the
   * {@linkplain TransactionMessage#getTransactionId() transaction messages}.
   */
  int value();
}
