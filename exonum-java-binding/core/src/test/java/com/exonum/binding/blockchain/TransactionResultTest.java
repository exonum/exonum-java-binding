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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.transaction.TransactionResult;
import com.exonum.binding.common.transaction.TransactionResult.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionResultTest {

  @Test
  void successful() {
    TransactionResult result = TransactionResult.successful();

    assertThat(result.getType()).isEqualTo(Type.SUCCESS);
    assertThat(result.getErrorCode()).isEmpty();
    assertThat(result.getErrorDescription()).isEmpty();
    assertTrue(result.isSuccessful());
  }

  @ParameterizedTest
  @ValueSource(ints = {
      0, 1, TransactionResult.MAX_USER_DEFINED_ERROR_CODE - 1,
      TransactionResult.MAX_USER_DEFINED_ERROR_CODE
  })
  void error(int errorCode) {
    String description = "Exception";

    TransactionResult result = TransactionResult.error(errorCode, description);

    assertThat(result.getType()).isEqualTo(Type.ERROR);
    assertThat(result.getErrorCode()).hasValue(errorCode);
    assertThat(result.getErrorDescription()).isEqualTo(description);
    assertFalse(result.isSuccessful());
  }

  @Test
  void errorNullDescription() {
    int errorCode = 1;
    TransactionResult result = TransactionResult.error(errorCode, null);

    assertThat(result.getErrorDescription()).isEmpty();
  }

  @Test
  void errorEmptyDescription() {
    int errorCode = 1;
    TransactionResult result = TransactionResult.error(errorCode, "");

    assertThat(result.getErrorDescription()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {
      Integer.MIN_VALUE,
      -1,
      TransactionResult.MAX_USER_DEFINED_ERROR_CODE + 1,
      Integer.MAX_VALUE
  })
  void invalidErrorCodes(int errorCode) {
    String description = "Exception";

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionResult.error(errorCode, description));

    assertThat(e).hasMessageContaining(Integer.toString(errorCode));
  }

  @Test
  void unexpectedError() {
    String description = "panic";

    TransactionResult result = TransactionResult.unexpectedError(description);

    assertThat(result.getType()).isEqualTo(Type.UNEXPECTED_ERROR);
    assertThat(result.getErrorCode()).isEmpty();
    assertThat(result.getErrorDescription()).isEqualTo(description);
    assertFalse(result.isSuccessful());
  }
}
