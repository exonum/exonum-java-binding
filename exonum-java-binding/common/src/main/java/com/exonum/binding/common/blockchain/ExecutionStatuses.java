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

package com.exonum.binding.common.blockchain;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.google.protobuf.Empty;

/**
 * Provides factory methods for creating some execution statuses, which represent a result
 * of the runtime operation execution (most often — service transaction execution).
 *
 * <p>Only the factory methods for the most common statuses are provided;
 * consider using the {@linkplain ExecutionStatus#newBuilder()} for other error kinds.
 *
 * @see ExecutionStatus
 */
public final class ExecutionStatuses {

  private static final ExecutionStatus SUCCESS = ExecutionStatus.newBuilder()
      .setOk(Empty.getDefaultInstance())
      .build();

  /**
   * Creates a successful execution status.
   */
  public static ExecutionStatus success() {
    return SUCCESS;
  }

  /**
   * Creates an execution status corresponding to a service-defined error with an empty description.
   * The status will have the <em>kind</em> field equal to {@link ErrorKind#SERVICE}.
   *
   * @param code a service-defined error code; must be non-negative
   */
  public static ExecutionStatus serviceError(int code) {
    return serviceError(code, "");
  }

  /**
   * Creates an execution status corresponding to a service-defined error.
   * The status will have the <em>kind</em> field equal to {@link ErrorKind#SERVICE}.
   *
   * @param code a service-defined error code; must be non-negative
   * @param description an error description; may be empty
   */
  public static ExecutionStatus serviceError(int code, String description) {
    checkArgument(0 <= code, "Error code (%s) must be non-negative", code);
    return ExecutionStatus.newBuilder()
        .setError(ExecutionError.newBuilder()
            .setKind(ErrorKind.SERVICE)
            .setCode(code)
            .setDescription(description))
        .build();
  }

  private ExecutionStatuses() {}
}
