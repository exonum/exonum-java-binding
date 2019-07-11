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

package com.exonum.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ExonumIterablesTest {

  @Test
  void indexOfFindsTheFirst() {
    List<Integer> l = ImmutableList.of(-1, 1, 2, 3);
    Predicate<Integer> positiveInt = i -> i > 0;
    assertThat(ExonumIterables.indexOf(l, positiveInt), equalTo(OptionalInt.of(1)));
  }

  @Test
  void indexOfNoMatches() {
    List<Integer> l = ImmutableList.of(-1, -2, -3);
    Predicate<Integer> positiveInt = i -> i > 0;
    assertThat(ExonumIterables.indexOf(l, positiveInt), equalTo(OptionalInt.empty()));
  }
}
