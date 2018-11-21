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

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.Optional;

/**
 * Returns a result of transaction execution. This result may be either a success, or an error,
 * if execution has failed. Errors consist of an error code and an optional description.
 */
public class TransactionResult implements Serializable {

  private Type type;
  private TransactionError error;

  public TransactionResult(Type type, TransactionError error) {
    this.type = type;
    this.error = error;
  }

  /**
   * Return whether transaction was successful or not.
   * @return true if transaction was successful, false otherwise
   */
  public boolean isSuccessful() {
    return type == Type.SUCCESS;
  }

  /**
   * Return type of the transaction.
   * @return {@code Type.SUCCESS} if transaction was successful
   *         {@code Type.ERROR} if there was an error during transaction execution
   *         {@code Type.UNEXPECTED_ERROR} if there was an error during transaction execution
   */
  public Type getType() {
    return type;
  }

  /**
   * Return a transaction error object of transaction if its execution resulted in an error.
   * @return a transaction error object, or {@code Optional.empty()} if transaction was successful
   */
  public Optional<TransactionError> getError() {
    return Optional.ofNullable(error);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionResult that = (TransactionResult) o;
    return type == that.type &&
        Objects.equal(error, that.error);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, error);
  }

  @Override
  public String toString() {
    return "TransactionResult{" +
        "type=" + type +
        ", error=" + error +
        '}';
  }

  public enum Type {
    SUCCESS,
    ERROR,
    UNEXPECTED_ERROR
  }
}
