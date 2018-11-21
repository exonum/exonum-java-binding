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

package com.exonum.binding.common.serialization;

import static com.exonum.binding.common.serialization.StandardSerializersTest.roundTripTest;

import com.exonum.binding.common.blockchain.TransactionError;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.blockchain.TransactionResult.Type;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionResultSerializerTest {
  private Serializer<TransactionResult> serializer = TransactionResultSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(TransactionResult key) {
    roundTripTest(key, serializer);
  }

  private static Stream<TransactionResult> testSource() {
    return Stream.of(
        new TransactionResult(Type.SUCCESS, null),
        new TransactionResult(Type.ERROR, new TransactionError((byte) 1, "Error description")),
        new TransactionResult(Type.UNEXPECTED_ERROR, new TransactionError((byte) 1, null)));
  }
}
