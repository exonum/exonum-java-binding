/*
 * Copyright 2020 The Exonum Team
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

package com.exonum.binding.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExecutionPreconditionsTest {
  private static final byte TEST_ERROR_CODE = 1;

  @Test
  void trueConditionDoesNothing() {
    ExecutionPreconditions.checkExecution(true, TEST_ERROR_CODE);
  }

  @Test
  void errorCodeIsPresent() {
    ExecutionException e = assertThrows(ExecutionException.class,
        () -> ExecutionPreconditions.checkExecution(false, TEST_ERROR_CODE));

    assertThat(e.getErrorCode()).isEqualTo(TEST_ERROR_CODE);
  }

  @Test
  void errorDescriptionIsPresent() {
    String description = "evil error";
    ExecutionException e = assertThrows(ExecutionException.class,
        () -> ExecutionPreconditions.checkExecution(false, TEST_ERROR_CODE, description));

    assertThat(e.getErrorCode()).isEqualTo(TEST_ERROR_CODE);
    assertThat(e).hasMessage(description);
  }

  @Test
  void nullableDescription() {
    ExecutionException e = assertThrows(ExecutionException.class,
        () -> ExecutionPreconditions.checkExecution(false, TEST_ERROR_CODE, null));

    assertThat(e).hasMessage("null");
  }

  @Test
  void errorDescriptionFormat() {
    int p1 = 10;
    int p2 = 20;

    ExecutionException e = assertThrows(ExecutionException.class,
        () -> ExecutionPreconditions.checkExecution(p1 == p2, TEST_ERROR_CODE, "%s != %s", p1, p2));

    assertThat(e).hasMessage("10 != 20");
  }
}
