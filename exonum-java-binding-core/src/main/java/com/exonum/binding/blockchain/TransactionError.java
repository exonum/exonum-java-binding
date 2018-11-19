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
 * Result of unsuccessful transaction execution.
 */
class TransactionError {

  private byte errorCode;
  private String description;

  TransactionError(byte errorCode, String description) {
    this.errorCode = errorCode;
    this.description = description;
  }

  byte getErrorCode() {
    return errorCode;
  }

  Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }
}
