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
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method is a transaction method. The annotated method should execute the
 * transaction, possibly modifying the blockchain state. The method should:
 * <ul>
 *   <li>be public
 *   <li>have exactly two parameters - the
 *       {@linkplain TransactionMessage#getPayload() serialized transaction arguments} of type
 *       'byte[]' and a transaction execution context, which allows to access the information about
 *       this transaction and modify the blockchain state through the included database fork of
 *       type '{@link TransactionContext}' in this particular order
 * </ul>
 *
 * <p>The annotated method might throw {@linkplain TransactionExecutionException} if the
 * transaction cannot be executed normally and has to be rolled back. The transaction will be
 * committed as failed (error kind {@linkplain ErrorKind#SERVICE SERVICE}), the
 * {@linkplain ExecutionError#getCode() error code} with the optional description will be saved
 * into the storage. The client can request the error code to know the reason of the failure.
 *
 * <p>The annotated method might also throw {@linkplain RuntimeException} if an unexpected error
 * occurs. A correct transaction implementation must not throw such exceptions. The transaction
 * will be committed as failed (status "panic").
 *
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/architecture/transactions">Exonum Transactions</a>
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/architecture/services">Exonum Services</a>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
// TODO: rename to Transaction after migration
public @interface TransactionMethod {

  /**
   * Returns the transaction type identifier which is unique within the service.
   */
  int value();
}
