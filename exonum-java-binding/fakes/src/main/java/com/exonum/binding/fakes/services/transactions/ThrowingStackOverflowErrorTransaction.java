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

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;

import java.util.LinkedList;
import java.util.List;

import static com.exonum.binding.common.serialization.StandardSerializers.*;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A transaction whose behaviour can be configured. It's not a mock: it writes a given value
 * into the database in its {@link #execute(TransactionContext)}.
 *
 * <p>Such transaction is supposed to be used in TransactionProxy integration tests.
 */
public final class ThrowingStackOverflowErrorTransaction implements Transaction {

  public final static int ID = 2;

  @Override
  public void execute(TransactionContext context) {

  }

  @Override
  public String info() {
    return "Non-convertable transaction";
  }

  public static Transaction fromArguments(byte[] arguments) {
    fromArguments(arguments);
    return new ThrowingStackOverflowErrorTransaction();
  }
}
