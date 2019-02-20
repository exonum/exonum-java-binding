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

package com.exonum.binding.blockchain.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.transaction.TransactionResult;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionResultSerializerTest {

  private static final Serializer<TransactionResult> serializer =
      TransactionResultSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(TransactionResult expected) {
    byte[] bytes = serializer.toBytes(expected);
    TransactionResult actual = serializer.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }

  private static Stream<TransactionResult> testSource() {
    return Stream.of(
        TransactionResult.successful(),
        TransactionResult.error(0, "Error description"),
        TransactionResult.error(1, /* Empty as no description: */ ""),
        TransactionResult.error(255, /* Null as no description: */ null),
        TransactionResult.unexpectedError("Boom"),
        TransactionResult.unexpectedError(""),
        TransactionResult.unexpectedError(null));
  }
}
