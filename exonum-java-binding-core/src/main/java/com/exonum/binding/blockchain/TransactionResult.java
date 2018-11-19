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

package com.exonum.binding.blockchain;

import java.util.Optional;

/**
 * Returns a result of transaction execution. This result may be either a success, or an error,
 * if execution has failed. Errors consist of an error code and an optional description.
 */
class TransactionResult {

  private Type type;
  private TransactionError error;

  @SuppressWarnings("unused")  // native API
  TransactionResult(Type type, TransactionError error) {
    this.type = type;
    this.error = error;
  }

  boolean isSuccessful() {
    return type == Type.SUCCESS;
  }

  Type getType() {
    return type;
  }

  Optional<TransactionError> getError() {
    return Optional.ofNullable(error);
  }

  enum Type {
    SUCCESS,
    ERROR,
    UNEXPECTED_ERROR
  }

}
