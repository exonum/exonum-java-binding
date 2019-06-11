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

package com.exonum.client.response;

import static com.exonum.client.Blocks.aBlock;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BlocksRangeTest {

  @ParameterizedTest
  @CsvSource({
      "-1, 1, 'from negative'",
      "0, -1, 'to negative'",
      "-2, -1, 'negative from & to'",
      "1, 0, 'from > to",
  })
  void constructorRejectsInvalidHeights(long from, long to,
      @SuppressWarnings("unused") String description) {
    List<Block> blocks = ImmutableList.of();
    assertThrows(IllegalArgumentException.class, () -> new BlocksRange(from, to, blocks));
  }

  @Test
  void invalidFirstBlock() {
    long from = 5;
    long to = 10;
    Block invalidBlock = aBlock()
        .height(from - 1)
        .build();
    List<Block> blocks = ImmutableList.of(invalidBlock);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new BlocksRange(from, to, blocks));
    String message = e.getMessage();
    assertThat(message, containsString(Long.toString(from)));
    assertThat(message, containsString(invalidBlock.toString()));
  }

  @Test
  void invalidLastBlock() {
    long from = 5;
    long to = 10;
    Block invalidBlock = aBlock()
        .height(to + 1)
        .build();
    List<Block> blocks = ImmutableList.of(invalidBlock);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new BlocksRange(from, to, blocks));
    String message = e.getMessage();
    assertThat(message, containsString(Long.toString(to)));
    assertThat(message, containsString(invalidBlock.toString()));
  }
}
