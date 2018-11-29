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

package com.exonum.binding.common.blockchain;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Returns a result of transaction execution. This result may be either a success, or an error,
 * if execution has failed. Errors consist of an error code and an optional description.
 */
@AutoValue
public abstract class TransactionResult {

  public static TransactionResult valueOf(Type type, @Nullable TransactionError error) {
    return new AutoValue_TransactionResult(type, Optional.ofNullable(error));
  }

  /**
   * Return type of the transaction.
   * @return {@code Type.SUCCESS} if transaction was successful
   *         {@code Type.ERROR} if there was an error during transaction execution
   *         {@code Type.UNEXPECTED_ERROR} if there was an unexpected error during transaction
   *         execution
   */
  public abstract Type getType();

  /**
   * Return a transaction error object of transaction if its execution resulted in an error.
   * @return a transaction error object, or {@code Optional.empty()} if transaction was successful
   */
  public abstract Optional<TransactionError> getError();

  /**
   * Return whether transaction was successful or not.
   * @return true if transaction was successful, false otherwise
   */
  public boolean isSuccessful() {
    return getType() == Type.SUCCESS;
  }

  public enum Type {
    SUCCESS,
    ERROR,
    UNEXPECTED_ERROR
  }
}
