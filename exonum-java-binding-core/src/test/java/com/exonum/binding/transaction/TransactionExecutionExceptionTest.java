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

package com.exonum.binding.transaction;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class TransactionExecutionExceptionTest {

  @Test
  void toStringNoDescription() {
    byte errorCode = 2;
    TransactionExecutionException e = new TransactionExecutionException(errorCode);

    assertThat(e.toString(),
        containsString("TransactionExecutionException: errorCode=2"));
  }

  @Test
  void toStringWithDescription() {
    byte errorCode = 2;
    String description = "Foo";
    TransactionExecutionException e = new TransactionExecutionException(errorCode, description);

    assertThat(e.toString(),
        containsString("TransactionExecutionException: Foo, errorCode=2"));
  }
}
