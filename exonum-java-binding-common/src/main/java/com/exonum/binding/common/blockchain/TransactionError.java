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
 * Result of unsuccessful transaction execution.
 */
public class TransactionError implements Serializable {

  private byte errorCode;
  private String description;

  public TransactionError(byte errorCode, String description) {
    this.errorCode = errorCode;
    this.description = description;
  }

  /**
   * Return an error code of a transaction error.
   * @return an error code of a transaction error
   */
  public byte getErrorCode() {
    return errorCode;
  }

  /**
   * Return an optional description of a transaction error.
   * @return a description of an error, or {@code Optional.empty()} otherwise
   */
  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionError that = (TransactionError) o;
    return errorCode == that.errorCode &&
        Objects.equal(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(errorCode, description);
  }

  @Override
  public String toString() {
    return "TransactionError{" +
        "errorCode=" + errorCode +
        ", description='" + description + '\'' +
        '}';
  }

}
