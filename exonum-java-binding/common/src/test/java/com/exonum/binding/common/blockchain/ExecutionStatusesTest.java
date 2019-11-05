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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExecutionStatusesTest {

  @Test
  void success() {
    ExecutionStatus s = ExecutionStatuses.success();
    assertTrue(s.hasOk());
  }

  @ParameterizedTest
  @MethodSource("errors")
  void serviceError(int code, String description) {
    ExecutionStatus s = ExecutionStatuses.serviceError(code, description);

    assertTrue(s.hasError());
    ExecutionError error = s.getError();
    assertThat(error.getKind()).isEqualTo(ErrorKind.SERVICE);
    assertThat(error.getCode()).isEqualTo(code);
    assertThat(error.getDescription()).isEqualTo(description);
  }

  private static List<Arguments> errors() {
    return ImmutableList.of(
        arguments(0, "Min error code"),
        arguments(1, ""),
        arguments(Integer.MAX_VALUE, "Max error code")
    );
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, -2, Integer.MIN_VALUE})
  void serviceErrorRejectsNegative(int code) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> ExecutionStatuses.serviceError(code, ""));

    assertThat(e).hasMessageContaining(String.valueOf(code));
  }
}
