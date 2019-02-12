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

package com.exonum.binding.fakes.mocks;

import com.exonum.binding.transaction.TransactionExecutionException;

import javax.annotation.Nullable;

/**
 * Used in tests that cover the cases of using subclass of {@link #TransactionExecutionException}.
 */
class TestTxExecException extends TransactionExecutionException {

  /**
   * The constructor that gets called from native code.
   *
   * @param errorCode the transaction error code
   * @param description the error description. The detail description is saved for
   *                    later retrieval by the {@link #getMessage()} method.
   */
  TestTxExecException(byte errorCode, @Nullable String description) {
    super(errorCode, description);
  }
}
