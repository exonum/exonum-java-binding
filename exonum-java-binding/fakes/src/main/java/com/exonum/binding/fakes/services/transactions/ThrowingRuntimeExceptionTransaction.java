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

package com.exonum.binding.fakes.services.transactions;

import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.google.common.io.BaseEncoding;

import static com.exonum.binding.common.serialization.StandardSerializers.*;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A transaction whose behaviour can be configured. It's not a mock: it writes a given value
 * into the database in its {@link #execute(TransactionContext)}.
 *
 * <p>Such transaction is supposed to be used in TransactionProxy integration tests.
 */
public final class ThrowingRuntimeExceptionTransaction implements Transaction {

  public final static int ID = 4;

  private final byte[] value;

  /**
   * Creates a transaction with a pre-configured behaviour.
   *
   * @param value a value to put into an entry {@link #ENTRY_NAME}
   */
  public ThrowingRuntimeExceptionTransaction(byte[] value) {
    this.value = checkNotNull(value);
  }

  @Override
  public void execute(TransactionContext context) {
    throw new ArithmeticException("on execute");
  }

  @Override
  public String info() {
    return BaseEncoding.base16().encode(value);
  }

  public static Transaction fromArguments(byte[] arguments) {
    return new ThrowingRuntimeExceptionTransaction(arguments);
  }
}
