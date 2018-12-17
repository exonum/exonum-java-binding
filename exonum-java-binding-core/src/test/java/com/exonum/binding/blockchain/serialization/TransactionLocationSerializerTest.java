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

import com.exonum.binding.blockchain.TransactionLocation;
import com.exonum.binding.common.serialization.Serializer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionLocationSerializerTest {

  private static final Serializer<TransactionLocation> serializer =
      TransactionLocationSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(TransactionLocation expected) {
    byte[] bytes = serializer.toBytes(expected);
    TransactionLocation actual = serializer.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }

  private static Stream<TransactionLocation> testSource() {
    return Stream.of(
        TransactionLocation.valueOf(1, 1),
        TransactionLocation.valueOf(Long.MAX_VALUE, Long.MAX_VALUE));
  }

}
