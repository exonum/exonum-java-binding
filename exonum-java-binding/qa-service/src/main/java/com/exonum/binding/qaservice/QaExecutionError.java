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

package com.exonum.binding.qaservice;

import com.google.common.primitives.UnsignedBytes;

enum QaExecutionError {
  // Create counter errors
  COUNTER_ALREADY_EXISTS(0),
  // Increment counter errors
  UNKNOWN_COUNTER(1),
  // Empty time oracle name supplied in the configuration
  EMPTY_TIME_ORACLE_NAME(2),
  // Error appeared while service resume
  RESUME_SERVICE_ERROR(3);

  final byte code;

  QaExecutionError(int code) {
    this.code = UnsignedBytes.checkedCast(code);
  }
}
