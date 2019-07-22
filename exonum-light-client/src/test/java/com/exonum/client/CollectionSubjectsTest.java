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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Correspondence;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class CollectionSubjectsTest {

  @Test
  void lists() {
    List<String> list = ImmutableList.of("hi", "Exonum", "service");

    assertThat(list).isNotEmpty();

    assertThat(list).contains("hi");
    assertThat(list).containsAtLeast("Exonum", "service");
    assertThat(list).containsExactly("hi", "service", "Exonum");
    assertThat(list).containsExactly("hi", "Exonum", "service").inOrder();
    // isEqualTo also works:
    assertThat(list).isEqualTo(asList("hi", "Exonum", "service"));

    // More obscure things:
    assertThat(list)
        .comparingElementsUsing(Correspondence.transforming(String::length, "length"))
        .contains(2);
  }

  @Test
  void mapsTest() {
    Map<String, Integer> map = ImmutableMap.of("hi", 1, "Exonum", 2, "service", 5);

    assertThat(map).containsEntry("hi", 1);
    assertThat(map).containsAtLeast("hi", 1, "Exonum", 2);
  }

  @Test
  void arrays() {
    String[] array = {"hi", "Exonum", "service"};

    assertThat(array).isNotEmpty();

    assertThat(array).hasLength(3);
    // Any other assertions require turning into a list
    assertThat(array).asList().containsAtLeast("Exonum", "service");
    assertThat(array).asList().containsExactly("hi", "service", "Exonum");
    assertThat(array).asList().containsExactly("hi", "Exonum", "service").inOrder();
    // isEqualTo also works:
    assertThat(array).isEqualTo(new String[]{"hi", "Exonum", "service"});
  }

  @Test
  void strings() {
    String message = "What on Earth has happened?!";

    assertThat(message).startsWith("W");
    assertThat(message).endsWith("?!");
    assertThat(message).contains("Earth");
    assertThat(message).ignoringCase().contains("WHAT");

    assertThat(message).containsMatch("Earth\\s?has");
    // No, use regexp flags instead:
    // assertThat(message).ignoringCase().containsMatch("Earth\\s?has");
    assertThat(message).containsMatch("(?i).*earth.*");
    assertThat(message).matches(".*Earth\\s?has.*");
  }

  @Test
  void throwables() {
    Throwable cause = new ArithmeticException("?");
    Throwable t = new IllegalArgumentException("What on Earth has happened?!", cause);

    assertThat(t).hasMessageThat().contains("Earth");
    assertThat(t).hasCauseThat().isInstanceOf(ArithmeticException.class);
    assertThat(t).hasCauseThat().hasMessageThat().isEqualTo("?");

//  Multiple assertions on the subject are not permitted, unlike in AssertJ:
//
//    //  AssertJ
//    assertThat(t).hasCauseThat()
//        .isInstanceOf(ArithmeticException.class)
//        .hasMessageThat().isEqualTo("?"); // won't compile
//
// On the other hand, AssertJ does not support Subjects*, therefore, tends to have longer
// method names and might miss some assertions (e.g., hasCauseThat returns a ThrowableSubject,
// which allows any ObjectSubject and ThrowableSubject assertions, but in AssertJ one is limited
// to assertions methods in the original subject):
//
//    //  AssertJ
//    assertThat(e).hasMessageContaining("Failed to load")
//        //        ^- Returns the original subject (ThrowableSubject), allowing multiple assertions…
//        .hasMessageFindingMatch("foo.+bar")
//        // ^- But you are limited to the methods in the original subject, which, if you want
          // to assert on strings, are more limited than in StringSubject
//        .hasNoCause();
//
// * — It supports, but mostly favours having methods for everything on the main subject
//     (allowing multiple assertions on it) instead of methods returning subject for a particular
//     feature (e.g., a StringSubject for an Exception message) preventing multiple assertions.
  }

  @Test
  void optionalsTest() {
    Optional<String> hiMaybe = Optional.empty();

    assertThat(hiMaybe).isPresent();
    assertThat(hiMaybe).hasValue("hi");
  }
}
