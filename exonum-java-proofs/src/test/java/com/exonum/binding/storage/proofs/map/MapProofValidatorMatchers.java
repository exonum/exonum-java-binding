/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.proofs.map;

import com.exonum.binding.hash.Hashing;
import javax.annotation.Nullable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

class MapProofValidatorMatchers {

  /**
   * Creates a matcher of a proof validator that is not valid
   * (i.e., its {@link MapProofValidator#isValid()} returns false).
   */
  static Matcher<MapProofValidator> isNotValid() {
    return new TypeSafeMatcher<MapProofValidator>() {

      @Override
      protected boolean matchesSafely(MapProofValidator proofValidator) {
        return !proofValidator.isValid();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("invalid proof");
      }
    };
  }

  /**
   * Creates a matcher of a proof validator that is valid and has
   * the same key and value as specified.
   *
   * @param key a requested key
   * @param expectedValue a value that is expected to be mapped to the requested key,
   *                      or null if there must not be such mapping in the proof map
   */
  static Matcher<MapProofValidator<String>> isValid(byte[] key, @Nullable String expectedValue) {
    return new TypeSafeMatcher<MapProofValidator<String>>() {
      private final Matcher<byte[]> keyMatcher = IsEqual.equalTo(key);
      private final Matcher<String> valueMatcher = IsEqual.equalTo(expectedValue);

      @Override
      protected boolean matchesSafely(MapProofValidator<String> item) {
        return item.isValid()
            && keyMatcher.matches(item.getKey())
            && item.getValue()
                .map(valueMatcher::matches)
                .orElse(expectedValue == null);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("valid proof, key=")
            .appendText(Hashing.toHexString(key))
            .appendText(", value=").appendText(expectedValue);
      }
    };
  }
}
