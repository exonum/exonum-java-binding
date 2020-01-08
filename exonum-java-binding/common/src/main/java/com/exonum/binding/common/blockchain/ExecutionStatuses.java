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

import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.google.protobuf.Empty;

/**
 * Provides pre-defined execution statuses.
 *
 * @see ExecutionStatus
 */
public final class ExecutionStatuses {

  /**
   * A successful execution status.
   */
  public static final ExecutionStatus SUCCESS = ExecutionStatus.newBuilder()
      .setOk(Empty.getDefaultInstance())
      .build();

  private ExecutionStatuses() {}
}
