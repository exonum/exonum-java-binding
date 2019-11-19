/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ${package};

import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.service.TransactionConverter;

/**
 * {@code MyTransactionConverter} converts transaction id and serialized transaction arguments
 * into {@linkplain Transaction executable transactions} of this service.
 */
public final class MyTransactionConverter implements TransactionConverter {

  @Override
  public Transaction toTransaction(int txId, byte[] arguments) {
    // TODO: implement transaction conversion
    throw new UnsupportedOperationException("Unimplemented");
  }

}
