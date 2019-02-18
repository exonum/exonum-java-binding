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
 *
 */

package com.exonum.client;

import static com.exonum.client.NodeUserAgentResponseParser.parseFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NodeUserAgentResponseParserTest {

  @ParameterizedTest
  @MethodSource("source")
  void parseFromTest(String input, NodeUserAgentResponse expected) {
    NodeUserAgentResponse actual = parseFrom(input);
    assertThat(actual, is(expected));
  }

  @Test
  void inputStringPartiallyFilledTest() {
    assertThrows(IllegalArgumentException.class, () -> parseFrom("exonum 0.6.0/"));
    assertThrows(IllegalArgumentException.class, () -> parseFrom("exonum 0.6.0/rustc 1.26.0 "));
  }

  @Test
  void invalidInputStringTest() {
    assertThrows(IllegalArgumentException.class, () -> parseFrom("invalid string"));
  }

  private static List<Arguments> source() {
    return ImmutableList.of(
        Arguments.of("exonum 0.6.0/rustc 1.26.0 (2789b067d 2018-03-06)\n\n/Mac OS10.13.3",
            new NodeUserAgentResponse("0.6.0", "1.26.0 (2789b067d 2018-03-06)",
                "Mac OS10.13.3")),
        Arguments.of("exonum 0.10.4/rustc 1.32.0 (9fda7c223 2019-01-16)\n\n/Mac OS10.14.3",
            new NodeUserAgentResponse("0.10.4", "1.32.0 (9fda7c223 2019-01-16)",
                "Mac OS10.14.3"))
    );
  }

}
