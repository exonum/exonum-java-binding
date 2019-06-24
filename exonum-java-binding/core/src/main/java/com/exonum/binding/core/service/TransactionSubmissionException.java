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

package com.exonum.binding.core.service;

import com.exonum.binding.core.transaction.RawTransaction;

/**
 * Indicates that a transaction could not be
 * {@linkplain Node#submitTransaction(RawTransaction) submitted}.
 * For example, the submitted transaction is not valid — belongs to an unknown service.
 */
public final class TransactionSubmissionException extends RuntimeException {

  public TransactionSubmissionException(String message) {
    super(message);
  }
}
