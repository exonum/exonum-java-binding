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
import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.Optional;

/**
 * Result of unsuccessful transaction execution.
 */
@AutoValue
public abstract class TransactionError implements Serializable {

  public static TransactionError valueOf(byte errorCode, String description) {
    return new AutoValue_TransactionError(errorCode, description);
  }

  /**
   * Return an error code of a transaction error.
   * @return an error code of a transaction error
   */
  public abstract byte getErrorCode();

  /**
   * Return an optional description of a transaction error.
   * @return a description of an error, or {@code Optional.empty()} otherwise
   */
  public abstract Optional<String> getDescription();

}
